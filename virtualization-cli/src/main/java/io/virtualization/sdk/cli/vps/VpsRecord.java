package io.virtualization.sdk.cli.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.vps.NetworkConfiguration;
import io.virtualization.sdk.vps.StorageConfiguration;
import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsState;
import io.virtualization.sdk.vps.VpsType;

import java.time.Instant;

/** JSON-serializable mirror of {@link Vps}, for {@link JsonFileVpsRepository}. See {@link VpsSpecRecord}. */
record VpsRecord(
        VpsId id,
        String name,
        VpsState state,
        VpsType type,
        ImageReference image,
        ComputeResources compute,
        StorageConfiguration storage,
        NetworkConfiguration network,
        VpsSpecRecord spec,
        String provider,
        String project,
        String workloadId,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant stoppedAt,
        Instant destroyedAt) {

    static VpsRecord from(Vps vps) {
        return new VpsRecord(
                vps.id(), vps.name(), vps.state(), vps.type(), vps.image(), vps.compute(), vps.storage(), vps.network(),
                VpsSpecRecord.from(vps.spec()), vps.provider(), vps.project(), vps.workloadId(), vps.createdAt(),
                vps.updatedAt(), vps.startedAt(), vps.stoppedAt(), vps.destroyedAt());
    }

    Vps toVps() {
        return new Vps(
                id, name, state, type, image, compute, storage, network, spec.toSpec(), provider, project, workloadId,
                createdAt, updatedAt, startedAt, stoppedAt, destroyedAt);
    }
}
