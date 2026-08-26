package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ProviderOptions;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The real {@link VpsProvisioner}: composes {@link ImageProvider} (fail-fast image existence
 * check) and {@link VirtualizationProvider#createFromImage} (which already resolves/pulls the
 * image per {@code ImageAvailabilityPolicy} internally — this class never touches pull/download
 * itself). Provider-neutral: works against any {@code VirtualizationProvider}/{@code
 * ImageProvider} pair, no Incus (or any other backend) dependency here.
 *
 * <p>{@code create}/{@code rebuild} run on a virtual thread, matching {@code
 * IncusProvider.createFromImage}'s own pattern: the calling thread never blocks, all work
 * (including the image check) happens on the background thread, and any {@link
 * VirtualizationException} it throws is routed to the returned operation's {@code fail}.
 */
public final class DefaultVpsProvisioner implements VpsProvisioner {

    // Discarded by DefaultVpsManager.mergeProvisioned (it only reads image/provider/project/
    // workloadId off the Vps this class hands back) — these only exist to satisfy Vps's non-null
    // constructor, their actual values don't matter.
    private static final ComputeResources PLACEHOLDER_COMPUTE = new ComputeResources(1, 1_024);
    private static final StorageConfiguration PLACEHOLDER_STORAGE = new StorageConfiguration(DataSize.ofGigabytes(10));

    private final VirtualizationProvider virtualizationProvider;
    private final ImageProvider imageProvider;

    public DefaultVpsProvisioner(VirtualizationProvider virtualizationProvider, ImageProvider imageProvider) {
        this.virtualizationProvider = Objects.requireNonNull(virtualizationProvider, "virtualizationProvider must not be null");
        this.imageProvider = Objects.requireNonNull(imageProvider, "imageProvider must not be null");
    }

    @Override
    public CreateVpsOperation create(VpsId id, VpsSpec spec) {
        CreateVpsHandle handle = CreateVpsHandle.create(id);
        Thread.ofVirtual().name("vps-create-" + id.value()).start(() -> runCreate(id, spec, handle));
        return handle.operation();
    }

    @Override
    public CreateVpsOperation rebuild(VpsId id, Vps current, ImageReference image) {
        CreateVpsHandle handle = CreateVpsHandle.create(id);
        Thread.ofVirtual().name("vps-rebuild-" + id.value()).start(() -> runRebuild(id, current, image, handle));
        return handle.operation();
    }

    @Override
    public Operation start(VpsId id, Vps current) {
        return virtualizationProvider.start(requireWorkloadId(current));
    }

    @Override
    public Operation stop(VpsId id, Vps current) {
        return virtualizationProvider.stop(requireWorkloadId(current));
    }

    @Override
    public Operation restart(VpsId id, Vps current) {
        return virtualizationProvider.reboot(requireWorkloadId(current));
    }

    @Override
    public Operation shutdown(VpsId id, Vps current) {
        return virtualizationProvider.shutdown(requireWorkloadId(current));
    }

    @Override
    public Operation destroy(VpsId id, Vps current) {
        return virtualizationProvider.destroy(requireWorkloadId(current));
    }

    private void runCreate(VpsId id, VpsSpec spec, CreateVpsHandle handle) {
        try {
            requireImageExists(spec.image());
            CreateWorkloadOperation workloadOperation = virtualizationProvider.createFromImage(spec.image(), toWorkloadSpec(spec));
            if (workloadOperation.await() == OperationStatus.FAILED) {
                handle.fail(workloadOperation.error()
                        .orElseGet(() -> new OperationException("VPS create failed for '" + id.value() + "'")));
                return;
            }
            handle.succeed(placeholderVps(id, spec, workloadOperation.workloadId().orElse(null)));
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
    }

    private void runRebuild(VpsId id, Vps current, ImageReference image, CreateVpsHandle handle) {
        try {
            requireImageExists(image);
            if (current.workloadId() != null && virtualizationProvider.destroy(current.workloadId()).await() == OperationStatus.FAILED) {
                handle.fail(new OperationException("Failed to destroy old workload for VPS '" + id.value() + "' during rebuild"));
                return;
            }
            VpsSpec rebuiltSpec = withImage(current.spec(), image);
            CreateWorkloadOperation workloadOperation = virtualizationProvider.createFromImage(image, toWorkloadSpec(rebuiltSpec));
            if (workloadOperation.await() == OperationStatus.FAILED) {
                handle.fail(workloadOperation.error()
                        .orElseGet(() -> new OperationException("VPS rebuild failed for '" + id.value() + "'")));
                return;
            }
            handle.succeed(placeholderVps(id, rebuiltSpec, workloadOperation.workloadId().orElse(null)));
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
    }

    private void requireImageExists(ImageReference image) {
        imageProvider.get(image).orElseThrow(() -> new ResourceNotFoundException(
                "No image '" + image.identifier() + "' on provider '" + image.provider() + "'"));
    }

    private Vps placeholderVps(VpsId id, VpsSpec spec, String workloadId) {
        Instant now = Instant.now();
        return new Vps(
                id, spec.name(), VpsState.READY, spec.type(), spec.image(),
                spec.compute().orElse(PLACEHOLDER_COMPUTE), spec.storage().orElse(PLACEHOLDER_STORAGE),
                spec.network().orElse(NetworkConfiguration.UNSPECIFIED), spec, virtualizationProvider.type().id(),
                spec.project().orElse(null), workloadId, now, now, null, null, null);
    }

    private static String requireWorkloadId(Vps current) {
        if (current.workloadId() == null) {
            throw new IllegalStateException("VPS '" + current.id().value() + "' has no backing workload yet");
        }
        return current.workloadId();
    }

    private static VpsSpec withImage(VpsSpec spec, ImageReference image) {
        VpsSpec.Builder builder = VpsSpec.builder(spec.name(), image).type(spec.type());
        spec.compute().ifPresent(builder::compute);
        spec.storage().ifPresent(builder::storage);
        spec.network().ifPresent(builder::network);
        builder.sshPublicKeys(spec.sshPublicKeys());
        spec.cloudInit().ifPresent(builder::cloudInit);
        builder.metadata(spec.metadata());
        builder.labels(spec.labels());
        spec.location().ifPresent(builder::location);
        spec.project().ifPresent(builder::project);
        spec.idempotencyKey().ifPresent(builder::idempotencyKey);
        return builder.build();
    }

    private static WorkloadSpec toWorkloadSpec(VpsSpec spec) {
        WorkloadSpec.Builder builder = WorkloadSpec.builder(spec.name(), toWorkloadType(spec.type())).image(spec.image());
        spec.compute().ifPresent(builder::resources);
        spec.storage().ifPresent(storage -> builder.storage(List.of(formatStorage(storage))));
        spec.network().map(NetworkConfiguration::network).filter(Objects::nonNull)
                .ifPresent(network -> builder.networks(List.of(network)));
        if (!spec.metadata().isEmpty()) {
            builder.metadata(spec.metadata());
        }
        if (!spec.labels().isEmpty()) {
            builder.labels(spec.labels());
        }
        Map<String, Object> providerOptions = providerOptions(spec);
        if (!providerOptions.isEmpty()) {
            builder.providerOptions(ProviderOptions.of(providerOptions));
        }
        return builder.build();
    }

    private static Map<String, Object> providerOptions(VpsSpec spec) {
        Map<String, Object> options = new LinkedHashMap<>();
        if (!spec.sshPublicKeys().isEmpty()) {
            options.put(VpsProviderOptionKeys.SSH_PUBLIC_KEYS, spec.sshPublicKeys());
        }
        spec.cloudInit().ifPresent(v -> options.put(VpsProviderOptionKeys.CLOUD_INIT, v));
        spec.network().ifPresent(network -> {
            if (network.hostname() != null) {
                options.put(VpsProviderOptionKeys.HOSTNAME, network.hostname());
            }
            if (network.ipv4() != null) {
                options.put(VpsProviderOptionKeys.IPV4, network.ipv4());
            }
            if (network.ipv6() != null) {
                options.put(VpsProviderOptionKeys.IPV6, network.ipv6());
            }
        });
        spec.storage().ifPresent(storage -> {
            if (storage.storagePool() != null) {
                options.put(VpsProviderOptionKeys.STORAGE_POOL, storage.storagePool());
            }
            if (storage.volumeType() != null) {
                options.put(VpsProviderOptionKeys.VOLUME_TYPE, storage.volumeType());
            }
        });
        spec.location().ifPresent(v -> options.put(VpsProviderOptionKeys.LOCATION, v));
        spec.project().ifPresent(v -> options.put(VpsProviderOptionKeys.PROJECT, v));
        return options;
    }

    private static String formatStorage(StorageConfiguration storage) {
        return "root:" + storage.rootDisk().toMegabytes() + "MB";
    }

    private static WorkloadType toWorkloadType(VpsType type) {
        return switch (type) {
            case CONTAINER -> WorkloadType.CONTAINER;
            case VIRTUAL_MACHINE -> WorkloadType.VIRTUAL_MACHINE;
        };
    }
}
