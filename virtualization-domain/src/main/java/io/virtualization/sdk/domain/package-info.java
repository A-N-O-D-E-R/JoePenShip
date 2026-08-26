/**
 * Provider-neutral domain registration: a {@link io.virtualization.sdk.domain.Domain} composes a
 * normalized name with an optional {@code virtualization-dns} {@link
 * io.virtualization.sdk.dns.DnsProvider} association, above which {@link
 * io.virtualization.sdk.domain.DomainManager} coordinates zone resolution and record management.
 * No certificates, ACME, Cloudflare, CLI, REST or Spring dependency here — those are separate,
 * later concerns. Pure Java, no framework dependencies.
 */
package io.virtualization.sdk.domain;
