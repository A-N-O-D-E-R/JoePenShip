package io.sablier.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DomainModelTest {

    @Test
    void workloadConstructsAndRejectsBlankIdGroupOrProject() {
        Workload workload = workload("w-1", "media");
        assertThat(workload.id()).isEqualTo("w-1");
        assertThat(workload.group()).isEqualTo("media");
        assertThat(workload.project()).isEqualTo("default");
        assertThat(workload.location()).isEmpty();

        assertThatIllegalArgumentException().isThrownBy(() -> workload("", "media"));
        assertThatIllegalArgumentException().isThrownBy(() -> workload("w-1", " "));
        assertThatIllegalArgumentException().isThrownBy(() -> new Workload(
                "w-1", "jellyfin", WorkloadType.CONTAINER, WorkloadState.RUNNING, "media", "", Optional.empty()));
        assertThatNullPointerException().isThrownBy(() -> new Workload(
                null, "jellyfin", WorkloadType.CONTAINER, WorkloadState.RUNNING, "media", "default", Optional.empty()));
    }

    private static Workload workload(String id, String group) {
        return new Workload(id, "jellyfin", WorkloadType.CONTAINER, WorkloadState.RUNNING, group, "default", Optional.empty());
    }

    @Test
    void workloadGroupConstructsAndRejectsBlankName() {
        Workload workload = workload("w-1", "media");
        WorkloadGroup group = new WorkloadGroup("media", List.of(workload));
        assertThat(group.workloads()).containsExactly(workload);

        assertThatIllegalArgumentException().isThrownBy(() -> new WorkloadGroup("", List.of()));
    }

    @Test
    void workloadMetadataConstructsAndRejectsBlankGroup() {
        WorkloadMetadata metadata = new WorkloadMetadata(true, "media", Optional.of(Duration.ofMinutes(30)), Optional.empty());
        assertThat(metadata.enabled()).isTrue();
        assertThat(metadata.defaultDuration()).hasValue(Duration.ofMinutes(30));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new WorkloadMetadata(true, "", Optional.empty(), Optional.empty()));
        assertThatNullPointerException()
                .isThrownBy(() -> new WorkloadMetadata(true, "media", null, Optional.empty()));
    }

    @Test
    void readinessStatusFactoriesProduceExpectedState() {
        assertThat(ReadinessStatus.ready().state()).isEqualTo(ReadinessState.READY);
        assertThat(ReadinessStatus.pending("waiting").state()).isEqualTo(ReadinessState.PENDING);
        assertThat(ReadinessStatus.failed("boom").state()).isEqualTo(ReadinessState.FAILED);
        assertThat(new ReadinessStatus(ReadinessState.PENDING, null).message()).isEmpty();
    }

    @Test
    void sessionRequestConstructsAndRejectsInvalidValues() {
        SessionRequest request = new SessionRequest("media", Duration.ofMinutes(30));
        assertThat(request.duration()).isEqualTo(Duration.ofMinutes(30));

        assertThatIllegalArgumentException().isThrownBy(() -> new SessionRequest("", Duration.ofMinutes(30)));
        assertThatIllegalArgumentException().isThrownBy(() -> new SessionRequest("media", Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new SessionRequest("media", Duration.ofMinutes(-1)));
    }

    @Test
    void sessionConstructsAndRejectsBlankIdOrGroup() {
        Instant now = Instant.now();
        Session session = new Session("s-1", "media", Optional.of("w-1"), now, now.plusSeconds(60), SessionStatus.ACTIVE);
        assertThat(session.workloadId()).hasValue("w-1");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Session("", "media", Optional.empty(), now, now, SessionStatus.ACTIVE));
        assertThatNullPointerException()
                .isThrownBy(() -> new Session("s-1", "media", null, now, now, SessionStatus.ACTIVE));
    }
}
