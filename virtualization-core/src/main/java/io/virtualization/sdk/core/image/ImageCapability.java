package io.virtualization.sdk.core.image;

/** An image operation an {@link ImageProvider} may or may not support. */
public enum ImageCapability {
    LIST,
    INSPECT,
    SEARCH,
    PULL,
    DOWNLOAD,
    UPLOAD,
    DELETE,
    INSTANTIATE,
    SNAPSHOT,
    PUBLISH
}
