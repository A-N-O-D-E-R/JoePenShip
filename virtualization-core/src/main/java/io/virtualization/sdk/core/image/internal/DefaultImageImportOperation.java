package io.virtualization.sdk.core.image.internal;

import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageImportHandle;
import io.virtualization.sdk.core.image.ImageImportOperation;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * Adds the byte-level detail and {@link Image} result {@link ImageImportOperation} declares on
 * top of {@link ComposedOperation}. Not part of the public API — obtain instances via {@link
 * ImageImportHandle#create(String)}.
 */
public final class DefaultImageImportOperation extends ComposedOperation implements ImageImportOperation, ImageImportHandle {

    private volatile long bytesTransferred = -1;
    private volatile long totalBytes = -1;
    private volatile Image result;

    public DefaultImageImportOperation(String id) {
        super(id);
    }

    @Override
    public ImageImportOperation operation() {
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
    public Optional<Image> result() {
        return Optional.ofNullable(result);
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
    public void succeed(Image result) {
        this.result = result;
        completeInternal();
    }

    @Override
    public void fail(VirtualizationException cause) {
        failInternal(cause);
    }
}
