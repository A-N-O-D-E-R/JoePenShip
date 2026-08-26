/**
 * Provider-neutral image management: image domain model, provider abstraction and capability
 * model. Mirrors the workload side of the SDK ({@link io.virtualization.sdk.core}) without
 * modifying it — {@link io.virtualization.sdk.core.image.ImageProvider} and
 * {@link io.virtualization.sdk.core.VirtualizationProvider} are independent abstractions an
 * application can inject separately.
 */
package io.virtualization.sdk.core.image;
