package io.sablier.core;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StateReadinessCheckerTest {

    private final StateReadinessChecker checker = new StateReadinessChecker();

    private static Workload workload(WorkloadState state) {
        return new Workload("w-1", "jellyfin", WorkloadType.CONTAINER, state, "media", "default", Optional.empty());
    }

    @Test
    void runningOrReadyStateIsReady() {
        assertThat(checker.check(workload(WorkloadState.RUNNING)).state()).isEqualTo(ReadinessState.READY);
        assertThat(checker.check(workload(WorkloadState.READY)).state()).isEqualTo(ReadinessState.READY);
    }

    @Test
    void anyOtherStateIsPending() {
        assertThat(checker.check(workload(WorkloadState.STARTING)).state()).isEqualTo(ReadinessState.PENDING);
        assertThat(checker.check(workload(WorkloadState.STOPPED)).state()).isEqualTo(ReadinessState.PENDING);
        assertThat(checker.check(workload(WorkloadState.ERROR)).state()).isEqualTo(ReadinessState.PENDING);
    }
}
