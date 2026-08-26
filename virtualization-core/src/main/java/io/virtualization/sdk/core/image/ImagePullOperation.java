package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.Operation;

import java.util.OptionalLong;

/**
 * A read-only view of an asynchronous {@link ImageProvider#pull(ImageReference)} in progress.
 * Extends the generic {@link Operation} view (status/progress/error/await) with byte-level detail
 * where a provider can report it.
 */
public interface ImagePullOperation extends Operation {

    /** Bytes transferred so far, if the provider reports it. */
    OptionalLong bytesTransferred();

    /** Total bytes expected, if known ahead of time. */
    OptionalLong totalBytes();
}
