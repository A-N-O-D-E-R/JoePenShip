package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.OperationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageImportOperationTest {

    private static final Image IMPORTED = new Image(
            new ImageId("sha256:abc"), "ubuntu", ImageType.CONTAINER, "x86_64", "ubuntu", "ubuntu", "24.04", 1024,
            Instant.now(), Map.of());

    @Test
    void succeedsWithImportedImage() {
        ImageImportHandle handle = ImageImportHandle.create("import-1");

        handle.updateBytes(100, 100);
        handle.succeed(IMPORTED);

        ImageImportOperation operation = handle.operation();
        assertThat(operation.await(Duration.ofSeconds(1))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(operation.result()).contains(IMPORTED);
        assertThat(operation.bytesTransferred()).hasValue(100);
    }

    @Test
    void failsWithoutResult() {
        ImageImportHandle handle = ImageImportHandle.create("import-2");

        handle.fail(new OperationException("import failed"));

        ImageImportOperation operation = handle.operation();
        assertThat(operation.await(Duration.ofSeconds(1))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.result()).isEmpty();
    }
}
