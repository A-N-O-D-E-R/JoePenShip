package io.virtualization.sdk.provisioning;

/**
 * How much of {@link VpsProvisioningService#provision} should run. {@code WEB_SERVER} is
 * currently identical to {@code HTTPS} — spec gives it no distinct behavior beyond the name yet.
 */
public enum VpsProvisioningProfile {
    /** VPS only — no DNS, no certificate, regardless of {@code VpsSpec}'s domain/DNS/TLS fields. */
    BASIC,
    /** VPS plus DNS records for {@link io.virtualization.sdk.vps.VpsSpec#domains()}, if any. */
    DOMAIN,
    /** {@code DOMAIN} plus a TLS certificate request, if {@code VpsSpec#tlsEnabled()}. */
    HTTPS,
    /** Currently identical to {@code HTTPS}. */
    WEB_SERVER
}
