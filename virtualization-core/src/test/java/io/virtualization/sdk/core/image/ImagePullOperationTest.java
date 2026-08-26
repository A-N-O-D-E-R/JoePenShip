package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.OperationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ImagePullOperationTest {

    @Test
    void reportsBytesAndDerivedProgress() {
        ImagePullHandle handle = ImagePullHandle.create("pull-1");

        handle.updateBytes(50, 200);

        ImagePullOperation operation = handle.operation();
        assertThat(operation.bytesTransferred()).hasValue(50);
        assertThat(operation.totalBytes()).hasValue(200);
        assertThat(operation.progress()).hasValue(0.25);
        assertThat(operation.status()).isEqualTo(OperationStatus.RUNNING);
    }

    @Test
    void completesSuccessfully() {
        ImagePullHandle handle = ImagePullHandle.create("pull-2");

        handle.complete();

        assertThat(handle.operation().await(Duration.ofSeconds(1))).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void reportsFailure() {
        ImagePullHandle handle = ImagePullHandle.create("pull-3");

        handle.fail(new OperationException("pull failed"));

        ImagePullOperation operation = handle.operation();
        assertThat(operation.await(Duration.ofSeconds(1))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).isPresent();
    }

    @Test
    void bytesAreEmptyUntilReported() {
        ImagePullHandle handle = ImagePullHandle.create("pull-4");

        assertThat(handle.operation().bytesTransferred()).isEmpty();
        assertThat(handle.operation().totalBytes()).isEmpty();
    }
}
