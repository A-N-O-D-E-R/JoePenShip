package io.virtualization.sdk.core;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageAvailabilityPolicy;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;

import java.util.List;

/**
 * A backend capable of managing virtual machines (Proxmox, Incus, QEMU, ...).
 *
 * <p>Not every provider supports every operation; check {@link #capabilities()} before invoking
 * one. An unsupported operation throws {@link UnsupportedCapabilityException} rather than
 * silently no-op-ing or throwing a generic {@link UnsupportedOperationException}.
 */
public interface VirtualizationProvider {

    ProviderType type();

    ProviderCapabilities capabilities();

    List<VirtualMachine> listVirtualMachines();

    /**
     * @throws ResourceNotFoundException if no virtual machine with the given id exists
     */
    VirtualMachine getVirtualMachine(String id);

    /** Providers with no container concept return an empty list. */
    default List<Container> listContainers() {
        return List.of();
    }

    /**
     * @throws ResourceNotFoundException if this provider does not support containers, or none with
     *     the given id exists
     */
    default Container getContainer(String id) {
        throw new ResourceNotFoundException("Provider '" + type().id() + "' does not support containers");
    }

    Operation start(String id);

    Operation stop(String id);

    Operation reboot(String id);

    Operation shutdown(String id);

    Operation destroy(String id);

    /**
     * Creates a workload (container or VM) from an image, pulling it first if missing (per
     * {@link ImageAvailabilityPolicy#PULL_IF_MISSING}, the default).
     *
     * @throws UnsupportedCapabilityException if this provider does not support creating workloads from images
     */
    default CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec) {
        return createFromImage(image, spec, ImageAvailabilityPolicy.PULL_IF_MISSING);
    }

    /**
     * @throws UnsupportedCapabilityException if this provider does not support creating workloads from images
     */
    default CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec, ImageAvailabilityPolicy policy) {
        throw new UnsupportedCapabilityException(type().id() + " does not support creating workloads from images");
    }
}
