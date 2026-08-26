package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * A read-only view of an asynchronous {@link ImageProvider#importImage(ImageSource)} in progress.
 */
public interface ImageImportOperation extends Operation {

    /** Bytes transferred so far, if the provider reports it. */
    OptionalLong bytesTransferred();

    /** Total bytes expected, if known ahead of time. */
    OptionalLong totalBytes();

    /** The imported {@link Image}, populated once this operation reaches {@link OperationStatus#SUCCEEDED}. */
    Optional<Image> result();
}
