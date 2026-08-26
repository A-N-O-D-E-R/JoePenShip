package io.virtualization.sdk.provisioning;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateManager;
import io.virtualization.sdk.certificate.CertificateRequest;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.domain.Domain;
import io.virtualization.sdk.domain.DomainId;
import io.virtualization.sdk.domain.DomainManager;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsRecordType;
import io.virtualization.sdk.dns.DnsZone;
import io.virtualization.sdk.vps.CreateVpsOperation;
import io.virtualization.sdk.vps.DnsProvisioningPolicy;
import io.virtualization.sdk.vps.NetworkConfiguration;
import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsManager;
import io.virtualization.sdk.vps.VpsSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Orchestrates {@link VpsManager}, {@link DomainManager} and {@link CertificateManager} — see the
 * package javadoc. Deliberately not folded into {@code VpsManager} itself: that would put ACME/DNS
 * orchestration inside a module that has (and must keep) zero knowledge of either.
 *
 * <p>DNS integration only works for a VPS with a caller-specified static IP ({@link
 * VpsSpec#network()}'s {@code ipv4}/{@code ipv6}) — nothing in this SDK retrieves a
 * provider-assigned/DHCP runtime IP yet, so a domain with no static IP configured simply has its
 * DNS record creation skipped (logged, not failed).
 */
public final class VpsProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(VpsProvisioningService.class);

    private final VpsManager vpsManager;
    private final DomainManager domainManager;
    private final CertificateManager certificateManager;

    public VpsProvisioningService(VpsManager vpsManager, DomainManager domainManager, CertificateManager certificateManager) {
        this.vpsManager = Objects.requireNonNull(vpsManager, "vpsManager must not be null");
        this.domainManager = Objects.requireNonNull(domainManager, "domainManager must not be null");
        this.certificateManager = Objects.requireNonNull(certificateManager, "certificateManager must not be null");
    }

    public ProvisioningResult provision(VpsSpec spec, VpsProvisioningProfile profile) {
        Objects.requireNonNull(spec, "spec must not be null");
        Objects.requireNonNull(profile, "profile must not be null");

        Vps vps = createVps(spec);

        List<Domain> domains = List.of();
        if (profile != VpsProvisioningProfile.BASIC && !spec.domains().isEmpty() && spec.dnsPolicy() != DnsProvisioningPolicy.NONE) {
            domains = provisionDns(spec, vps);
        }

        Certificate certificate = null;
        boolean httpsProfile = profile == VpsProvisioningProfile.HTTPS || profile == VpsProvisioningProfile.WEB_SERVER;
        if (httpsProfile && spec.tlsEnabled() && !spec.domains().isEmpty()) {
            certificate = requestCertificate(spec);
        }

        return new ProvisioningResult(vps, domains, certificate);
    }

    private Vps createVps(VpsSpec spec) {
        CreateVpsOperation operation = vpsManager.create(spec);
        operation.await();
        if (operation.status() == OperationStatus.FAILED) {
            throw operation.error().orElseGet(() -> new OperationException("VPS provisioning failed for '" + spec.name() + "'"));
        }
        return vpsManager.get(operation.vpsId());
    }

    private List<Domain> provisionDns(VpsSpec spec, Vps vps) {
        String dnsProviderName = spec.dnsProvider()
                .orElseThrow(() -> new IllegalStateException("dnsPolicy is " + spec.dnsPolicy() + " but no dnsProvider was set"));
        List<Domain> domains = new ArrayList<>();
        for (String rawDomain : spec.domains()) {
            Domain domain = findOrRegisterDomain(rawDomain);
            if (!dnsProviderName.equals(domain.dnsProvider())) {
                domain = domainManager.associateDnsProvider(domain.id(), dnsProviderName);
            }
            domains.add(domain);
            createOrUpdateRecords(spec, vps, domain);
        }
        return domains;
    }

    /**
     * ponytail: find-then-register-if-absent, not atomic under concurrent callers (same
     * single-JVM-scope caveat {@code DefaultVpsManager}'s own idempotency doc already accepts) —
     * {@link DomainManager#register} itself has no dedup, a pre-existing gap this phase works
     * around locally rather than fixing (a different, separate concern from what Phase 6 needs).
     */
    private Domain findOrRegisterDomain(String rawDomain) {
        return domainManager.findByName(rawDomain).orElseGet(() -> domainManager.register(rawDomain));
    }

    private void createOrUpdateRecords(VpsSpec spec, Vps vps, Domain domain) {
        NetworkConfiguration network = vps.network();
        if (network.ipv4() == null && network.ipv6() == null) {
            log.info("Skipping DNS record for domain '{}' — VPS '{}' has no static IP configured", domain.name(), vps.id().value());
            return;
        }
        DnsZone zone = domainManager.resolveZone(domain.id())
                .orElseThrow(() -> new ResourceNotFoundException("No DNS zone found for domain '" + domain.name() + "'"));
        String label = recordLabel(domain.name(), zone.name());

        if (network.ipv4() != null) {
            upsertRecord(spec, domain.id(), new DnsRecordSpec(label, DnsRecordType.A, network.ipv4(), null, null));
        }
        if (network.ipv6() != null) {
            upsertRecord(spec, domain.id(), new DnsRecordSpec(label, DnsRecordType.AAAA, network.ipv6(), null, null));
        }
    }

    /** {@code "@"} is the conventional apex-record name (Cloudflare/Route53/BIND zone files all accept it). */
    private static String recordLabel(String domain, String zoneName) {
        return domain.equals(zoneName) ? "@" : domain.substring(0, domain.length() - zoneName.length() - 1);
    }

    private void upsertRecord(VpsSpec spec, DomainId domainId, DnsRecordSpec recordSpec) {
        if (spec.dnsPolicy() == DnsProvisioningPolicy.CREATE_AND_UPDATE) {
            Optional<DnsRecord> existing = domainManager.listRecords(domainId).stream()
                    .filter(r -> r.name().equals(recordSpec.name()) && r.type() == recordSpec.type())
                    .findFirst();
            if (existing.isPresent()) {
                domainManager.updateRecord(domainId, existing.get().id(), recordSpec);
                return;
            }
        }
        domainManager.createRecord(domainId, recordSpec);
    }

    private Certificate requestCertificate(VpsSpec spec) {
        String issuer = spec.tlsCertificateIssuer()
                .orElseThrow(() -> new IllegalStateException("tlsEnabled is true but no tlsCertificateIssuer was set"));
        CertificateRequest request = CertificateRequest.builder().domains(spec.domains()).issuer(issuer).build();
        return certificateManager.requestCertificate(request);
    }
}
