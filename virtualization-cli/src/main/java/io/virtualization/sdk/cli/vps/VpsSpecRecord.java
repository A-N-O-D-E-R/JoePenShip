package io.virtualization.sdk.cli.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.vps.NetworkConfiguration;
import io.virtualization.sdk.vps.StorageConfiguration;
import io.virtualization.sdk.vps.VpsSpec;
import io.virtualization.sdk.vps.VpsType;

import java.util.List;
import java.util.Map;

/**
 * JSON-serializable mirror of {@link VpsSpec} — a plain record round-trips with Jackson for free;
 * {@code VpsSpec} itself can't (builder-only, no Jackson creator, and deliberately has no Jackson
 * dependency at all — {@code virtualization-vps} stays framework-free).
 */
record VpsSpecRecord(
        String name,
        ImageReference image,
        VpsType type,
        ComputeResources compute,
        StorageConfiguration storage,
        NetworkConfiguration network,
        List<String> sshPublicKeys,
        String cloudInit,
        Map<String, String> metadata,
        Map<String, String> labels,
        String location,
        String project,
        String idempotencyKey) {

    static VpsSpecRecord from(VpsSpec spec) {
        return new VpsSpecRecord(
                spec.name(), spec.image(), spec.type(), spec.compute().orElse(null), spec.storage().orElse(null),
                spec.network().orElse(null), spec.sshPublicKeys(), spec.cloudInit().orElse(null), spec.metadata(),
                spec.labels(), spec.location().orElse(null), spec.project().orElse(null), spec.idempotencyKey().orElse(null));
    }

    VpsSpec toSpec() {
        VpsSpec.Builder builder = VpsSpec.builder(name, image).type(type);
        if (compute != null) {
            builder.compute(compute);
        }
        if (storage != null) {
            builder.storage(storage);
        }
        if (network != null) {
            builder.network(network);
        }
        builder.sshPublicKeys(sshPublicKeys != null ? sshPublicKeys : List.of());
        if (cloudInit != null) {
            builder.cloudInit(cloudInit);
        }
        builder.metadata(metadata != null ? metadata : Map.of());
        builder.labels(labels != null ? labels : Map.of());
        if (location != null) {
            builder.location(location);
        }
        if (project != null) {
            builder.project(project);
        }
        if (idempotencyKey != null) {
            builder.idempotencyKey(idempotencyKey);
        }
        return builder.build();
    }
}
