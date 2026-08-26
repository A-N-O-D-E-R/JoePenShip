/**
 * Orchestrates {@link io.virtualization.sdk.vps.VpsManager}, {@link
 * io.virtualization.sdk.domain.DomainManager} and {@link
 * io.virtualization.sdk.certificate.CertificateManager} into one {@link
 * io.virtualization.sdk.provisioning.VpsProvisioningService#provision} call, gated by a {@link
 * io.virtualization.sdk.provisioning.VpsProvisioningProfile}. {@code virtualization-vps} itself
 * stays fully decoupled from domains/certificates — this module is the only place that composes
 * both trees. No certificate deployment onto the VPS yet, no CLI/REST/Spring wiring here (later
 * phases). Pure Java, no framework dependencies.
 */
package io.virtualization.sdk.provisioning;
