package io.virtualization.sdk.cli.support;

import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageCapabilities;
import io.virtualization.sdk.core.image.ImageCapability;
import io.virtualization.sdk.core.image.ImageDownload;
import io.virtualization.sdk.core.image.ImageId;
import io.virtualization.sdk.core.image.ImageImportHandle;
import io.virtualization.sdk.core.image.ImageImportOperation;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImagePullHandle;
import io.virtualization.sdk.core.image.ImagePullOperation;
import io.virtualization.sdk.core.image.ImageQuery;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ImageSource;
import io.virtualization.sdk.core.image.ImageType;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Hand-written test double for {@link ImageProvider}. Serves one canned image ("ubuntu/24.04"). */
public final class FakeImageProvider implements ImageProvider {

    public static final String NAME = "fake";

    private final Image image = new Image(
            new ImageId("fp-1"), "ubuntu/24.04", ImageType.CONTAINER, "x86_64", "ubuntu", "ubuntu", "24.04", 1024,
            Instant.parse("2024-01-01T00:00:00Z"), Map.of());
    private final AtomicInteger operationCounter = new AtomicInteger();
    private volatile boolean nextOperationFails;

    public void nextOperationFails() {
        this.nextOperationFails = true;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ImageCapabilities capabilities() {
        return ImageCapabilities.of(
                ImageCapability.LIST, ImageCapability.INSPECT, ImageCapability.SEARCH, ImageCapability.PULL,
                ImageCapability.DOWNLOAD, ImageCapability.UPLOAD);
    }

    @Override
    public List<Image> list() {
        return List.of(image);
    }

    @Override
    public Optional<Image> get(ImageReference reference) {
        return reference.identifier().equals(image.name()) ? Optional.of(image) : Optional.empty();
    }

    @Override
    public List<Image> search(ImageQuery query) {
        return query.distribution().map(d -> d.equals(image.distribution())).orElse(true) ? List.of(image) : List.of();
    }

    @Override
    public ImagePullOperation pull(ImageReference reference) {
        ImagePullHandle handle = ImagePullHandle.create("pull-" + operationCounter.incrementAndGet());
        if (consumeFailureFlag()) {
            handle.fail(new OperationException("simulated failure"));
        } else {
            handle.complete();
        }
        return handle.operation();
    }

    @Override
    public ImageDownload download(ImageReference reference) {
        byte[] data = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);
        return ImageDownload.of(new ByteArrayInputStream(data), data.length, "application/octet-stream", "abc123", "sha256");
    }

    @Override
    public ImageImportOperation importImage(ImageSource source) {
        ImageImportHandle handle = ImageImportHandle.create("import-" + operationCounter.incrementAndGet());
        if (consumeFailureFlag()) {
            handle.fail(new OperationException("simulated failure"));
        } else {
            handle.succeed(image);
        }
        return handle.operation();
    }

    private boolean consumeFailureFlag() {
        boolean fail = nextOperationFails;
        nextOperationFails = false;
        return fail;
    }
}
