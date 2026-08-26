package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.OperationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class CreateWorkloadOperationTest {

    @Test
    void succeedsWithWorkloadId() {
        CreateWorkloadHandle handle = CreateWorkloadHandle.create("create-1");

        handle.updateProgress(0.5);
        handle.succeed("ubuntu-test");

        CreateWorkloadOperation operation = handle.operation();
        assertThat(operation.await(Duration.ofSeconds(1))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(operation.workloadId()).contains("ubuntu-test");
    }

    @Test
    void failsWithoutWorkloadId() {
        CreateWorkloadHandle handle = CreateWorkloadHandle.create("create-2");

        handle.fail(new OperationException("create failed"));

        CreateWorkloadOperation operation = handle.operation();
        assertThat(operation.await(Duration.ofSeconds(1))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.workloadId()).isEmpty();
    }
}
