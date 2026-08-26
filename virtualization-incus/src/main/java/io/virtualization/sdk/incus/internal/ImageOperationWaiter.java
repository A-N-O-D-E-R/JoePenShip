package io.virtualization.sdk.incus.internal;

import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.CreateWorkloadHandle;
import io.virtualization.sdk.core.image.ImageImportHandle;
import io.virtualization.sdk.core.image.ImagePullHandle;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.client.dto.ImageDto;
import io.virtualization.sdk.incus.client.dto.OperationStatusDto;

import java.time.Duration;
import java.util.OptionalDouble;

/**
 * Drives an {@link ImagePullHandle}, {@link ImageImportHandle} or {@link CreateWorkloadHandle} to
 * completion using Incus's long-polling {@code /operations/{id}/wait?timeout=N} endpoint, the
 * same way {@link OperationWaiter} does for lifecycle operations — but also surfacing the pull
 * progress text and resulting fingerprint Incus reports in the operation's metadata. Blocking:
 * callers run this on their own thread (a virtual thread, for {@code IncusImageProvider} /
 * {@code IncusProvider}).
 */
public final class ImageOperationWaiter {

    private ImageOperationWaiter() {}

    public static void waitPull(IncusApiClient client, String operationId, ImagePullHandle handle, Duration waitTimeout) {
        try {
            while (true) {
                OperationStatusDto status = poll(client, operationId, waitTimeout);
                parseProgress(status.downloadProgress()).ifPresent(handle::updateProgress);
                if (status.isFinished()) {
                    if (status.isSuccessful()) {
                        handle.complete();
                    } else {
                        handle.fail(new OperationException("Incus pull operation " + operationId + " failed: " + status.err()));
                    }
                    return;
                }
            }
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
    }

    public static void waitImport(IncusApiClient client, String operationId, ImageImportHandle handle, Duration waitTimeout) {
        try {
            while (true) {
                OperationStatusDto status = poll(client, operationId, waitTimeout);
                if (status.isFinished()) {
                    if (!status.isSuccessful()) {
                        handle.fail(new OperationException("Incus import operation " + operationId + " failed: " + status.err()));
                        return;
                    }
                    String fingerprint = status.fingerprint();
                    if (fingerprint == null) {
                        handle.fail(new OperationException(
                                "Incus import operation " + operationId + " succeeded without a fingerprint"));
                        return;
                    }
                    ImageDto dto = client.getSingle("/images/" + fingerprint, ImageDto.class);
                    handle.succeed(IncusImageMapper.toImage(dto));
                    return;
                }
            }
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
    }

    public static void waitCreate(
            IncusApiClient client, String operationId, CreateWorkloadHandle handle, String workloadId, Duration waitTimeout) {
        try {
            while (true) {
                OperationStatusDto status = poll(client, operationId, waitTimeout);
                if (status.isFinished()) {
                    if (status.isSuccessful()) {
                        handle.succeed(workloadId);
                    } else {
                        handle.fail(new OperationException("Incus create operation " + operationId + " failed: " + status.err()));
                    }
                    return;
                }
            }
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
    }

    private static OperationStatusDto poll(IncusApiClient client, String operationId, Duration waitTimeout) {
        return client.getSingle("/operations/" + operationId + "/wait?timeout=" + waitTimeout.toSeconds(), OperationStatusDto.class);
    }

    private static OptionalDouble parseProgress(String downloadProgress) {
        if (downloadProgress == null) {
            return OptionalDouble.empty();
        }
        int percentIndex = downloadProgress.indexOf('%');
        if (percentIndex <= 0) {
            return OptionalDouble.empty();
        }
        try {
            double percent = Double.parseDouble(downloadProgress.substring(0, percentIndex).trim());
            return OptionalDouble.of(Math.min(1.0, percent / 100.0));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }
}
