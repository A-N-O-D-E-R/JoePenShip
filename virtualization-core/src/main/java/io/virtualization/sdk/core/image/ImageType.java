package io.virtualization.sdk.core.image;

/**
 * The kind of artifact an {@link Image} represents. Not every provider supports every type (e.g.
 * Docker/Podman images are {@link #OCI}; Incus images are {@link #CONTAINER} or
 * {@link #VIRTUAL_MACHINE}; QEMU disk images are {@link #DISK}).
 */
public enum ImageType {
    CONTAINER,
    VIRTUAL_MACHINE,
    DISK,
    ISO,
    OCI,
    UNKNOWN
}
