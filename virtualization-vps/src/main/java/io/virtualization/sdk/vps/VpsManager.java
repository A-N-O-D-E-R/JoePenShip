package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.ImageReference;

import java.util.List;

/** Provider-neutral VPS lifecycle: create/get/list/start/stop/restart/shutdown/destroy/rebuild. */
public interface VpsManager {

    CreateVpsOperation create(VpsSpec spec);

    /**
     * @throws ResourceNotFoundException if no VPS with the given id exists
     */
    Vps get(VpsId id);

    List<Vps> list();

    Operation start(VpsId id);

    Operation stop(VpsId id);

    Operation restart(VpsId id);

    Operation shutdown(VpsId id);

    Operation destroy(VpsId id);

    CreateVpsOperation rebuild(VpsId id, ImageReference image);
}
