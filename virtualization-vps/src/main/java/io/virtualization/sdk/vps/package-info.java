/**
 * Provider-neutral VPS management: a {@link io.virtualization.sdk.vps.Vps} composes an image,
 * compute, storage, network and lifecycle into one orchestrated resource, above {@code
 * VirtualizationProvider}/{@code ImageProvider} — no Incus, Proxmox or QEMU dependency here.
 * Pure Java, no framework dependencies. Provider modules implement {@link
 * io.virtualization.sdk.vps.VpsProvisioner} to do the actual provisioning.
 */
package io.virtualization.sdk.vps;
