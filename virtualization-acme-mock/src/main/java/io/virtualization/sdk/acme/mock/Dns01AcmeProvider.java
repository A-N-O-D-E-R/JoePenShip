package io.virtualization.sdk.acme.mock;

import io.virtualization.sdk.certificate.AcmeProvider;
import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.certificate.CertificateMaterial;
import io.virtualization.sdk.certificate.CertificateRequest;
import io.virtualization.sdk.certificate.CertificateRequestHandle;
import io.virtualization.sdk.certificate.CertificateRequestOperation;
import io.virtualization.sdk.certificate.CertificateStatus;
import io.virtualization.sdk.certificate.CertificateStore;
import io.virtualization.sdk.certificate.ChallengeType;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.dns.DnsProvider;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsRecordType;
import io.virtualization.sdk.dns.DnsZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A real {@link AcmeProvider} implementing DNS-01 (RFC 8555 §8.4) over a composed {@link
 * DnsProvider} — not a real certificate authority (no wire protocol, no cryptography), but a
 * genuine end-to-end exercise of the challenge/validate/cleanup workflow: creates a
 * {@code _acme-challenge.<domain>} TXT record per requested domain, "validates" (nothing external
 * to wait on here), issues a certificate, and always cleans the TXT records up afterward — success
 * or failure, via {@code try/finally}.
 */
public final class Dns01AcmeProvider implements AcmeProvider {

    private static final Logger log = LoggerFactory.getLogger(Dns01AcmeProvider.class);
    private static final Duration VALIDITY = Duration.ofDays(90);
    private static final long CHALLENGE_TTL_SECONDS = 60;

    private final DnsProvider dnsProvider;
    private final CertificateStore certificateStore;
    private final Map<CertificateId, Certificate> issued = new ConcurrentHashMap<>();

    public Dns01AcmeProvider(DnsProvider dnsProvider, CertificateStore certificateStore) {
        this.dnsProvider = Objects.requireNonNull(dnsProvider, "dnsProvider must not be null");
        this.certificateStore = Objects.requireNonNull(certificateStore, "certificateStore must not be null");
    }

    @Override
    public CertificateRequestOperation request(CertificateRequest request) {
        if (request.challenge() != ChallengeType.DNS_01) {
            throw new UnsupportedCapabilityException("Dns01AcmeProvider only supports " + ChallengeType.DNS_01 + ", got " + request.challenge());
        }
        CertificateRequestHandle handle = CertificateRequestHandle.create(CertificateId.generate());
        Thread.ofVirtual().name("acme-dns01-" + handle.operation().certificateId().value()).start(() -> run(request, handle));
        return handle.operation();
    }

    private void run(CertificateRequest request, CertificateRequestHandle handle) {
        List<CreatedRecord> created = new ArrayList<>();
        Certificate certificate = null;
        VirtualizationException failure = null;
        try {
            for (String domain : request.domains()) {
                ZoneMatch match = resolveZone(domain);
                DnsRecordSpec spec = new DnsRecordSpec(challengeRecordName(match.label()), DnsRecordType.TXT, syntheticToken(), CHALLENGE_TTL_SECONDS, null);
                DnsRecord record = dnsProvider.createRecord(match.zone().name(), spec);
                created.add(new CreatedRecord(match.zone().name(), record.id()));
            }
            // mock CA: no real external validation to wait on — proceed straight to issuance.
            Instant now = Instant.now();
            certificate = new Certificate(
                    handle.operation().certificateId(), CertificateStatus.ACTIVE, request.domains(), now, now.plus(VALIDITY), request.issuer());
            certificateStore.store(certificate.id(), syntheticMaterial(certificate));
            issued.put(certificate.id(), certificate);
        } catch (VirtualizationException e) {
            failure = e;
        } finally {
            // cleanup must complete before the operation is marked terminal below — succeed()/
            // fail() unblocks any awaiting caller, who must never observe a not-yet-cleaned-up
            // challenge record.
            cleanup(created);
        }
        if (failure != null) {
            handle.fail(failure);
        } else {
            handle.succeed(certificate);
        }
    }

    /**
     * {@link DnsProvider#getZone} is an exact-match lookup — walk up the label chain (e.g. {@code
     * app.example.com} -> {@code example.com}) until a registered zone matches, since a
     * certificate domain is usually a subdomain of the zone that actually owns it, not the zone
     * apex itself.
     */
    private ZoneMatch resolveZone(String domain) {
        String candidate = domain;
        StringBuilder label = new StringBuilder();
        while (true) {
            Optional<DnsZone> zone = dnsProvider.getZone(candidate);
            if (zone.isPresent()) {
                return new ZoneMatch(zone.get(), label.toString());
            }
            int dot = candidate.indexOf('.');
            if (dot < 0) {
                throw new ResourceNotFoundException("No DNS zone owns domain '" + domain + "'");
            }
            if (!label.isEmpty()) {
                label.append('.');
            }
            label.append(candidate, 0, dot);
            candidate = candidate.substring(dot + 1);
        }
    }

    /** RFC 8555 §8.4: the challenge record always lives at {@code _acme-challenge.<domain>}, apex included. */
    private static String challengeRecordName(String label) {
        return label.isEmpty() ? "_acme-challenge" : "_acme-challenge." + label;
    }

    private void cleanup(List<CreatedRecord> created) {
        // ponytail: log-and-continue, no retry-with-backoff — proportionate for a mock provider;
        // add real retry if a production ACME module ever needs it.
        for (CreatedRecord record : created) {
            try {
                dnsProvider.deleteRecord(record.zone(), record.recordId());
            } catch (VirtualizationException e) {
                log.warn("Failed to clean up DNS-01 challenge record '{}' in zone '{}': {}", record.recordId(), record.zone(), e.getMessage());
            }
        }
    }

    private static String syntheticToken() {
        return "acme-challenge-" + UUID.randomUUID();
    }

    private static CertificateMaterial syntheticMaterial(Certificate certificate) {
        String subject = String.join(",", certificate.domains());
        return new CertificateMaterial(
                "-----BEGIN CERTIFICATE-----\n" + subject + "\n-----END CERTIFICATE-----",
                "-----BEGIN PRIVATE KEY-----\n" + certificate.id().value() + "\n-----END PRIVATE KEY-----",
                "-----BEGIN CERTIFICATE-----\nmock-chain\n-----END CERTIFICATE-----");
    }

    @Override
    public Certificate get(CertificateId id) {
        return requireIssued(id);
    }

    @Override
    public void revoke(Certificate current) {
        issued.put(current.id(), new Certificate(
                current.id(), CertificateStatus.REVOKED, current.domains(), current.issuedAt(), current.expiresAt(), current.issuer()));
    }

    @Override
    public Certificate renew(Certificate current) {
        Instant now = Instant.now();
        Certificate renewed = new Certificate(current.id(), CertificateStatus.ACTIVE, current.domains(), now, now.plus(VALIDITY), current.issuer());
        issued.put(current.id(), renewed);
        return renewed;
    }

    private Certificate requireIssued(CertificateId id) {
        Certificate certificate = issued.get(id);
        if (certificate == null) {
            throw new ResourceNotFoundException("No certificate with id '" + id.value() + "'");
        }
        return certificate;
    }

    private record ZoneMatch(DnsZone zone, String label) {}

    private record CreatedRecord(String zone, String recordId) {}
}
