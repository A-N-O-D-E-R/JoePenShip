package io.virtualization.sdk.core;

/**
 * Compute resources allocated to a {@link VirtualMachine} or {@link Container}.
 *
 * @param cpuCores  number of CPU cores, must be positive
 * @param memoryMb  memory in megabytes, must be non-negative
 */
public record ComputeResources(int cpuCores, long memoryMb) {

    public ComputeResources {
        if (cpuCores <= 0) {
            throw new IllegalArgumentException("cpuCores must be positive, was " + cpuCores);
        }
        if (memoryMb < 0) {
            throw new IllegalArgumentException("memoryMb must not be negative, was " + memoryMb);
        }
    }
}
