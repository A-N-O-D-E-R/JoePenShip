package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.image.ImageReference;

/**
 * Does the actual provisioning work for a {@link VpsManager} — the Phase 1/Phase 2 seam. Phase 1
 * tests drive {@link DefaultVpsManager} against a hand-written fake; a later phase implements this
 * for real by composing {@code ImageProvider} + {@code VirtualizationProvider}. Never implemented
 * by a specific backend module directly against this interface's caller — {@code
 * virtualization-vps} has no compile dependency on Incus or any other provider module.
 */
public interface VpsProvisioner {

    CreateVpsOperation create(VpsId id, VpsSpec spec);

    CreateVpsOperation rebuild(VpsId id, Vps current, ImageReference image);

    Operation start(VpsId id, Vps current);

    Operation stop(VpsId id, Vps current);

    Operation restart(VpsId id, Vps current);

    Operation shutdown(VpsId id, Vps current);

    Operation destroy(VpsId id, Vps current);
}
