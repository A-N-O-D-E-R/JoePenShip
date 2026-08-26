package io.virtualization.sdk.incus;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageAvailabilityPolicy;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ProviderOptions;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.support.FakeIncusServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IncusProviderCreateFromImageTest {

    private FakeIncusServer server;
    private IncusProvider provider;

    @BeforeEach
    void startServer() {
        server = new FakeIncusServer().start();
        server.addImage(
                "fp-ubuntu-2404", "container", "x86_64", Map.of("os", "Ubuntu", "release", "24.04"), 512_000L,
                "2024-04-01T00:00:00Z", List.of("ubuntu/24.04"));

        IncusApiClient client = new IncusApiClient(HttpClient.newHttpClient(), server.uri(), Duration.ofSeconds(5));
        IncusImageProvider images = new IncusImageProvider(client, "local");
        provider = new IncusProvider(client, Duration.ofSeconds(1), images);
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void createsFromLocalImageByFingerprint() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER).build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(operation.workloadId()).contains("ubuntu-test");

        Map<String, Object> request = server.lastInstanceCreateRequest().orElseThrow();
        assertThat(request.get("name")).isEqualTo("ubuntu-test");
        assertThat(request.get("type")).isEqualTo("container");
        Map<?, ?> source = (Map<?, ?>) request.get("source");
        assertThat(source.get("fingerprint")).isEqualTo("fp-ubuntu-2404");
    }

    @Test
    void createFromMissingLocalImageFails() {
        WorkloadSpec spec = WorkloadSpec.builder("missing-test", WorkloadType.CONTAINER).build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "debian/13"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).containsInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pullIfMissingLetsIncusFetchDuringCreate() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-remote", WorkloadType.CONTAINER)
                .resources(new io.virtualization.sdk.core.ComputeResources(2, 2048))
                .build();

        CreateWorkloadOperation operation = provider.createFromImage(
                new ImageReference("incus", "images", "ubuntu/24.04"), spec, ImageAvailabilityPolicy.PULL_IF_MISSING);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);

        Map<String, Object> request = server.lastInstanceCreateRequest().orElseThrow();
        Map<?, ?> source = (Map<?, ?>) request.get("source");
        assertThat(source.get("alias")).isEqualTo("ubuntu/24.04");
        assertThat(source.get("server")).isEqualTo("https://images.linuxcontainers.org");
        assertThat(source.get("protocol")).isEqualTo("simplestreams");
        Map<?, ?> config = (Map<?, ?>) request.get("config");
        assertThat(config.get("limits.cpu")).isEqualTo("2");
        assertThat(config.get("limits.memory")).isEqualTo("2048MB");
    }

    @Test
    void requireLocalRejectsRemoteReference() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-remote", WorkloadType.CONTAINER).build();

        CreateWorkloadOperation operation = provider.createFromImage(
                new ImageReference("incus", "images", "ubuntu/24.04"), spec, ImageAvailabilityPolicy.REQUIRE_LOCAL);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).containsInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void alwaysRefreshPullsFirstThenCreatesFromResolvedFingerprint() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-refreshed", WorkloadType.CONTAINER).build();

        CreateWorkloadOperation operation = provider.createFromImage(
                new ImageReference("incus", "images", "ubuntu/24.04"), spec, ImageAvailabilityPolicy.ALWAYS_REFRESH);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(server.lastPullRequest()).isPresent();

        Map<String, Object> request = server.lastInstanceCreateRequest().orElseThrow();
        Map<?, ?> source = (Map<?, ?>) request.get("source");
        assertThat(source.get("fingerprint")).isEqualTo("pulled-ubuntu-24.04");
    }

    @Test
    void unknownRemoteFails() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-bad-remote", WorkloadType.CONTAINER).build();

        CreateWorkloadOperation operation = provider.createFromImage(
                new ImageReference("incus", "no-such-remote", "ubuntu/24.04"), spec, ImageAvailabilityPolicy.PULL_IF_MISSING);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).containsInstanceOf(UnsupportedCapabilityException.class);
    }

    @Test
    void createsVirtualMachineType() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-vm", WorkloadType.VIRTUAL_MACHINE).build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(server.lastInstanceCreateRequest().orElseThrow().get("type")).isEqualTo("virtual-machine");
    }

    @Test
    void explicitCloudInitIsUsedVerbatim() {
        String userData = "#cloud-config\nusers:\n  - name: admin\n";
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER)
                .providerOptions(ProviderOptions.of(Map.of("cloudInit", userData, "sshPublicKeys", List.of("ssh-ed25519 AAAA"))))
                .build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        Map<?, ?> config = (Map<?, ?>) server.lastInstanceCreateRequest().orElseThrow().get("config");
        // explicit cloudInit wins outright — sshPublicKeys is not merged into it.
        assertThat(config.get("cloud-init.user-data")).isEqualTo(userData);
    }

    @Test
    void sshPublicKeysAndHostnameSynthesizeCloudConfig() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER)
                .providerOptions(ProviderOptions.of(Map.of(
                        "sshPublicKeys", List.of("ssh-ed25519 AAAA", "ssh-ed25519 BBBB"), "hostname", "web-01")))
                .build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        Map<?, ?> config = (Map<?, ?>) server.lastInstanceCreateRequest().orElseThrow().get("config");
        String userData = (String) config.get("cloud-init.user-data");
        assertThat(userData).startsWith("#cloud-config\n");
        assertThat(userData).contains("hostname: \"web-01\"").contains("fqdn: \"web-01\"");
        assertThat(userData).contains("ssh_authorized_keys:")
                .contains("  - \"ssh-ed25519 AAAA\"")
                .contains("  - \"ssh-ed25519 BBBB\"");
    }

    @Test
    void hostnameWithQuotesAndNewlinesIsYamlEscaped() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER)
                .providerOptions(ProviderOptions.of(Map.of("hostname", "evil\"\nruncmd: [rm -rf /]")))
                .build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        Map<?, ?> config = (Map<?, ?>) server.lastInstanceCreateRequest().orElseThrow().get("config");
        String userData = (String) config.get("cloud-init.user-data");
        // the embedded quote/newline stay escaped inside one scalar, not breaking out into a new
        // top-level YAML key — the raw "runcmd:" directive must not appear unescaped.
        assertThat(userData).doesNotContain("\nruncmd:");
        assertThat(userData).contains("evil\\\"\\nruncmd: [rm -rf /]");
    }

    @Test
    void noCloudInitOptionsLeavesConfigUnset() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER).build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        Map<?, ?> config = (Map<?, ?>) server.lastInstanceCreateRequest().orElseThrow().get("config");
        assertThat(config.get("cloud-init.user-data")).isNull();
    }

    @Test
    void storageEntryAndStoragePoolBuildRootDiskDevice() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER)
                .storage(List.of("root:40960MB"))
                .providerOptions(ProviderOptions.of(Map.of("storagePool", "fast")))
                .build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        Map<?, ?> devices = (Map<?, ?>) server.lastInstanceCreateRequest().orElseThrow().get("devices");
        Map<?, ?> root = (Map<?, ?>) devices.get("root");
        assertThat(root.get("type")).isEqualTo("disk");
        assertThat(root.get("path")).isEqualTo("/");
        assertThat(root.get("size")).isEqualTo("40960MB");
        assertThat(root.get("pool")).isEqualTo("fast");
    }

    @Test
    void networkAndStaticAddressesBuildNicDevice() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER)
                .networks(List.of("default"))
                .providerOptions(ProviderOptions.of(Map.of("ipv4", "10.0.0.5", "ipv6", "fd00::5")))
                .build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        Map<?, ?> devices = (Map<?, ?>) server.lastInstanceCreateRequest().orElseThrow().get("devices");
        Map<?, ?> eth0 = (Map<?, ?>) devices.get("eth0");
        assertThat(eth0.get("type")).isEqualTo("nic");
        assertThat(eth0.get("network")).isEqualTo("default");
        assertThat(eth0.get("ipv4.address")).isEqualTo("10.0.0.5");
        assertThat(eth0.get("ipv6.address")).isEqualTo("fd00::5");
    }

    @Test
    void noStorageOrNetworkLeavesDevicesEmpty() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER).build();

        CreateWorkloadOperation operation = provider.createFromImage(new ImageReference("incus", "local", "ubuntu/24.04"), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        Map<?, ?> devices = (Map<?, ?>) server.lastInstanceCreateRequest().orElseThrow().get("devices");
        assertThat(devices).isEmpty();
    }
}
