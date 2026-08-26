package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.internal.DefaultImagePullOperation;

/**
 * The producer side of an {@link ImagePullOperation}, used by provider modules to drive a pull to
 * completion. Mirrors {@link OperationHandle}.
 */
public interface ImagePullHandle {

    /** The read-only view of this operation, to hand to SDK consumers. */
    ImagePullOperation operation();

    /**
     * Reports progress as a fraction between 0.0 and 1.0.
     *
     * @throws IllegalArgumentException if progress is outside {@code [0.0, 1.0]}
     */
    void updateProgress(double progress);

    /** Reports byte-level progress, when the provider can measure it. */
    void updateBytes(long bytesTransferred, long totalBytes);

    /** Marks the pull as successfully completed. */
    void complete();

    /** Marks the pull as failed with the given cause. */
    void fail(VirtualizationException cause);

    static ImagePullHandle create(String id) {
        return new DefaultImagePullOperation(id);
    }
}
