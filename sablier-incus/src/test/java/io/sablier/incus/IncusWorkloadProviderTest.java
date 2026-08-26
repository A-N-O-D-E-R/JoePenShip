package io.sablier.incus;

import io.sablier.core.Operation;
import io.sablier.core.OperationStatus;
import io.sablier.core.ReadinessState;
import io.sablier.core.Workload;
import io.sablier.core.WorkloadState;
import io.sablier.core.exception.WorkloadNotFoundException;
import io.sablier.incus.support.FakeIncusServer;
import io.sablier.incus.support.TestTlsFixtures;
import io.virtualization.sdk.incus.IncusClientConfig;
import io.virtualization.sdk.incus.IncusProvider;
import io.virtualization.sdk.incus.IncusTlsCredentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncusWorkloadProviderTest {

    private FakeIncusServer server;
    private IncusWorkloadProvider provider;

    @BeforeEach
    void startServer() {
        server = new FakeIncusServer().start();
        IncusTlsCredentials credentials =
                new IncusTlsCredentials(TestTlsFixtures.CLIENT_CERTIFICATE_PEM, TestTlsFixtures.CLIENT_KEY_PEM);
        IncusProvider incusProvider = new IncusProvider(IncusClientConfig.of(server.uri(), credentials));
        provider = new IncusWorkloadProvider(incusProvider, "default", Duration.ofSeconds(1));
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    private void enabledInstance(String name, String group, String status, String location) {
        server.addInstance(
                "default", name, "container", status, location, Map.of("user.sablier.enable", "true", "user.sablier.group", group));
    }

    @Test
    void listOnlyReturnsSablierEnabledInstances() {
        enabledInstance("jellyfin", "media", "Running", "node1");
        server.addInstance("default", "not-enabled", "container", "Running", "", Map.of());
        server.addInstance("default", "enabled-no-group", "container", "Running", "", Map.of("user.sablier.enable", "true"));

        List<Workload> workloads = provider.list();

        assertThat(workloads).extracting(Workload::id).containsExactly("jellyfin");
    }

    @Test
    void listedWorkloadCarriesGroupProjectLocationAndState() {
        enabledInstance("jellyfin", "media", "Running", "node1");

        Workload workload = provider.list().getFirst();

        assertThat(workload.group()).isEqualTo("media");
        assertThat(workload.project()).isEqualTo("default");
        assertThat(workload.location()).hasValue("node1");
        assertThat(workload.state()).isEqualTo(WorkloadState.RUNNING);
    }

    @Test
    void findByGroupFiltersAcrossInstances() {
        enabledInstance("jellyfin", "media", "Running", "");
        enabledInstance("sonarr", "media", "Stopped", "");
        enabledInstance("gitea", "dev", "Running", "");

        assertThat(provider.findByGroup("media")).extracting(Workload::id).containsExactlyInAnyOrder("jellyfin", "sonarr");
        assertThat(provider.findByGroup("dev")).extracting(Workload::id).containsExactly("gitea");
        assertThat(provider.findByGroup("nonexistent")).isEmpty();
    }

    @Test
    void getReturnsWorkloadForEnabledInstance() {
        enabledInstance("jellyfin", "media", "Running", "");

        Workload workload = provider.get("jellyfin");

        assertThat(workload.name()).isEqualTo("jellyfin");
    }

    @Test
    void getThrowsWorkloadNotFoundForNonEnabledInstance() {
        server.addInstance("default", "not-enabled", "container", "Running", "", Map.of());

        assertThatThrownBy(() -> provider.get("not-enabled")).isInstanceOf(WorkloadNotFoundException.class);
    }

    @Test
    void getThrowsWorkloadNotFoundForUnknownInstance() {
        assertThatThrownBy(() -> provider.get("missing")).isInstanceOf(WorkloadNotFoundException.class);
    }

    @Test
    void stateMapsIncusStatusToWorkloadState() {
        enabledInstance("jellyfin", "media", "Stopped", "");

        assertThat(provider.state("jellyfin")).isEqualTo(WorkloadState.STOPPED);
    }

    @Test
    void readinessReflectsRunningState() {
        enabledInstance("jellyfin", "media", "Running", "");
        enabledInstance("sonarr", "media", "Stopped", "");

        assertThat(provider.readiness("jellyfin").state()).isEqualTo(ReadinessState.READY);
        assertThat(provider.readiness("sonarr").state()).isEqualTo(ReadinessState.PENDING);
    }

    @Test
    void startCompletesSuccessfullyOnceIncusOperationFinishes() {
        enabledInstance("jellyfin", "media", "Stopped", "");
        server.pollsBeforeCompletion(2);

        Operation operation = provider.start("jellyfin");

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void stopReportsFailureWhenIncusOperationFails() {
        enabledInstance("jellyfin", "media", "Running", "");
        server.nextOperationFails();

        Operation operation = provider.stop("jellyfin");

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).isPresent();
    }

    @Test
    void startOnUnknownWorkloadThrowsWorkloadNotFound() {
        assertThatThrownBy(() -> provider.start("missing")).isInstanceOf(WorkloadNotFoundException.class);
    }
}
