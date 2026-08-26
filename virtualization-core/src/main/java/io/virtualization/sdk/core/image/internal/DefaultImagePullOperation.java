package io.virtualization.sdk.core.image.internal;

import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.ImagePullHandle;
import io.virtualization.sdk.core.image.ImagePullOperation;

import java.util.OptionalLong;

/**
 * Adds the byte-level detail {@link ImagePullOperation} declares on top of {@link
 * ComposedOperation}. Not part of the public API — obtain instances via {@link
 * ImagePullHandle#create(String)}.
 */
public final class DefaultImagePullOperation extends ComposedOperation implements ImagePullOperation, ImagePullHandle {

    private volatile long bytesTransferred = -1;
    private volatile long totalBytes = -1;

    public DefaultImagePullOperation(String id) {
        super(id);
    }

    @Override
    public ImagePullOperation operation() {
        return this;
    }

    @Override
    public OptionalLong bytesTransferred() {
        long value = bytesTransferred;
        return value < 0 ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @Override
    public OptionalLong totalBytes() {
        long value = totalBytes;
        return value < 0 ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @Override
    public void updateProgress(double progress) {
        updateProgressInternal(progress);
    }

    @Override
    public void updateBytes(long bytesTransferred, long totalBytes) {
        this.bytesTransferred = bytesTransferred;
        this.totalBytes = totalBytes;
        if (totalBytes > 0) {
            updateProgressInternal(Math.min(1.0, (double) bytesTransferred / totalBytes));
        }
    }

    @Override
    public void complete() {
        completeInternal();
    }

    @Override
    public void fail(VirtualizationException cause) {
        failInternal(cause);
    }
}
