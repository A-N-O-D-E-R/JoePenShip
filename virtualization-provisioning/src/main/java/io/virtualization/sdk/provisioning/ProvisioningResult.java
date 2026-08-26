package io.virtualization.sdk.provisioning;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.domain.Domain;
import io.virtualization.sdk.vps.Vps;

import java.util.List;
import java.util.Objects;

/**
 * What {@link VpsProvisioningService#provision} actually did. {@code domains} is empty (never
 * {@code null}) when the DNS step didn't run; {@code certificate} is {@code null} when the TLS
 * step didn't run.
 */
public record ProvisioningResult(Vps vps, List<Domain> domains, Certificate certificate) {

    public ProvisioningResult {
        Objects.requireNonNull(vps, "vps must not be null");
        domains = domains == null ? List.of() : List.copyOf(domains);
    }
}
