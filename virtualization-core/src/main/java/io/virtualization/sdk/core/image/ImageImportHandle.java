package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.internal.DefaultImageImportOperation;

/**
 * The producer side of an {@link ImageImportOperation}, used by provider modules to drive an
 * import to completion. Mirrors {@link OperationHandle}.
 */
public interface ImageImportHandle {

    /** The read-only view of this operation, to hand to SDK consumers. */
    ImageImportOperation operation();

    /**
     * Reports progress as a fraction between 0.0 and 1.0.
     *
     * @throws IllegalArgumentException if progress is outside {@code [0.0, 1.0]}
     */
    void updateProgress(double progress);

    /** Reports byte-level progress, when the provider can measure it. */
    void updateBytes(long bytesTransferred, long totalBytes);

    /** Marks the import as successfully completed, with the resulting image. */
    void succeed(Image result);

    /** Marks the import as failed with the given cause. */
    void fail(VirtualizationException cause);

    static ImageImportHandle create(String id) {
        return new DefaultImageImportOperation(id);
    }
}
