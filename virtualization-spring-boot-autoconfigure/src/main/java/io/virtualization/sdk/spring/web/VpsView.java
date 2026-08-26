package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsState;
import io.virtualization.sdk.vps.VpsType;

import java.time.Instant;

record VpsView(
        String id,
        String name,
        VpsState state,
        VpsType type,
        String imageProvider,
        String imageRemote,
        String imageIdentifier,
        int cpuCores,
        long memoryMb,
        long diskMb,
        String storagePool,
        String volumeType,
        String network,
        String ipv4,
        String ipv6,
        String hostname,
        String provider,
        String project,
        String workloadId,
        Instant createdAt,
        Instant updatedAt,
        Instant startedAt,
        Instant stoppedAt,
        Instant destroyedAt) {

    static VpsView from(Vps vps) {
        return new VpsView(
                vps.id().value(), vps.name(), vps.state(), vps.type(),
                vps.image().provider(), vps.image().remote(), vps.image().identifier(),
                vps.compute().cpuCores(), vps.compute().memoryMb(),
                vps.storage().rootDisk().toMegabytes(), vps.storage().storagePool(), vps.storage().volumeType(),
                vps.network().network(), vps.network().ipv4(), vps.network().ipv6(), vps.network().hostname(),
                vps.provider(), vps.project(), vps.workloadId(),
                vps.createdAt(), vps.updatedAt(), vps.startedAt(), vps.stoppedAt(), vps.destroyedAt());
    }
}
