package io.sablier.core;

import io.sablier.core.exception.OperationException;
import io.sablier.core.exception.SablierException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperationTest {

    @Test
    void startsRunningWithNoProgressOrError() {
        OperationHandle handle = OperationHandle.create("op-1");
        Operation operation = handle.operation();

        assertThat(operation.id()).isEqualTo("op-1");
        assertThat(operation.status()).isEqualTo(OperationStatus.RUNNING);
        assertThat(operation.progress()).isEmpty();
        assertThat(operation.error()).isEmpty();
    }

    @Test
    void updateProgressRejectsOutOfRangeValues() {
        OperationHandle handle = OperationHandle.create("op-1");

        assertThatIllegalArgumentException().isThrownBy(() -> handle.updateProgress(-0.1));
        assertThatIllegalArgumentException().isThrownBy(() -> handle.updateProgress(1.1));

        handle.updateProgress(0.5);
        assertThat(handle.operation().progress()).hasValue(0.5);
    }

    @Test
    void completeMarksSucceededAndUnblocksAwait() {
        OperationHandle handle = OperationHandle.create("op-1");
        handle.complete();

        Operation operation = handle.operation();
        assertThat(operation.status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(operation.await()).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void failMarksFailedWithoutAwaitThrowing() {
        OperationHandle handle = OperationHandle.create("op-1");
        SablierException cause = new SablierException("boom");
        handle.fail(cause);

        Operation operation = handle.operation();
        assertThat(operation.status()).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).hasValue(cause);
        assertThat(operation.await()).isEqualTo(OperationStatus.FAILED);
    }

    @Test
    void awaitWithTimeoutSucceedsWhenCompletedFromAnotherThread() throws InterruptedException {
        OperationHandle handle = OperationHandle.create("op-1");
        Thread completer = new Thread(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            handle.complete();
        });
        completer.start();

        OperationStatus status = handle.operation().await(Duration.ofSeconds(5));

        assertThat(status).isEqualTo(OperationStatus.SUCCEEDED);
        completer.join();
    }

    @Test
    void awaitWithTimeoutThrowsWhenNeverCompleted() {
        OperationHandle handle = OperationHandle.create("op-1");

        assertThatThrownBy(() -> handle.operation().await(Duration.ofMillis(50)))
                .isInstanceOf(OperationException.class);
    }
}
