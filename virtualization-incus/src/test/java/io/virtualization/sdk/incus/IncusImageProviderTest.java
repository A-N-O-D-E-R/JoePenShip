package io.virtualization.sdk.incus;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageAlias;
import io.virtualization.sdk.core.image.ImageDownload;
import io.virtualization.sdk.core.image.ImageImportOperation;
import io.virtualization.sdk.core.image.ImagePullOperation;
import io.virtualization.sdk.core.image.ImageQuery;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ImageType;
import io.virtualization.sdk.core.image.InputStreamImageSource;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.support.FakeIncusServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncusImageProviderTest {

    private FakeIncusServer server;
    private IncusImageProvider provider;

    @BeforeEach
    void startServer() {
        server = new FakeIncusServer().start();
        server.addImage(
                "fp-ubuntu-2404", "container", "x86_64", Map.of("os", "Ubuntu", "release", "24.04"), 512_000L,
                "2024-04-01T00:00:00Z", List.of("ubuntu/24.04"));
        server.addImage(
                "fp-debian-13", "virtual-machine", "x86_64", Map.of("os", "Debian", "release", "13"), 900_000L,
                "2024-06-01T00:00:00Z", List.of("debian/13"));

        IncusApiClient client = new IncusApiClient(HttpClient.newHttpClient(), server.uri(), Duration.ofSeconds(5));
        provider = new IncusImageProvider(client, "local");
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void listReturnsAllImagesMapped() {
        List<Image> images = provider.list();

        assertThat(images).hasSize(2);
        Image ubuntu = images.stream().filter(i -> i.name().equals("ubuntu/24.04")).findFirst().orElseThrow();
        assertThat(ubuntu.id().value()).isEqualTo("fp-ubuntu-2404");
        assertThat(ubuntu.type()).isEqualTo(ImageType.CONTAINER);
        assertThat(ubuntu.architecture()).isEqualTo("x86_64");
        assertThat(ubuntu.distribution()).isEqualTo("Ubuntu");
        assertThat(ubuntu.version()).isEqualTo("24.04");
        assertThat(ubuntu.size()).isEqualTo(512_000L);
        assertThat(ubuntu.createdAt()).isEqualTo(Instant.parse("2024-04-01T00:00:00Z"));
        assertThat(ubuntu.metadata()).containsEntry("incus.public", "false");
    }

    @Test
    void getResolvesByAlias() {
        Optional<Image> image = provider.get(new ImageReference("incus", "local", "ubuntu/24.04"));

        assertThat(image).isPresent();
        assertThat(image.get().id().value()).isEqualTo("fp-ubuntu-2404");
    }

    @Test
    void getResolvesByFingerprint() {
        Optional<Image> image = provider.get(new ImageReference("incus", "local", "fp-debian-13"));

        assertThat(image).isPresent();
        assertThat(image.get().type()).isEqualTo(ImageType.VIRTUAL_MACHINE);
    }

    @Test
    void getReturnsEmptyForUnknownIdentifier() {
        assertThat(provider.get(new ImageReference("incus", "local", "missing"))).isEmpty();
    }

    @Test
    void getReturnsEmptyForMismatchedRemote() {
        assertThat(provider.get(new ImageReference("incus", "images", "ubuntu/24.04"))).isEmpty();
    }

    @Test
    void searchFiltersByDistributionAndType() {
        List<Image> results = provider.search(ImageQuery.builder().distribution("Ubuntu").build());

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("ubuntu/24.04");

        assertThat(provider.search(ImageQuery.builder().type(ImageType.VIRTUAL_MACHINE).build())).hasSize(1);
        assertThat(provider.search(ImageQuery.builder().distribution("Fedora").build())).isEmpty();
        assertThat(provider.search(ImageQuery.builder().remote("images").build())).isEmpty();
    }

    @Test
    void aliasesListsEveryImageAlias() {
        List<ImageAlias> aliases = provider.aliases();

        assertThat(aliases).extracting(ImageAlias::name).containsExactlyInAnyOrder("ubuntu/24.04", "debian/13");
    }

    @Test
    void pullSubmitsSourceFromResolvedRemoteAndCompletes() {
        server.pullProgress("50% (5.0MB/10.0MB)");

        ImagePullOperation operation = provider.pull(new ImageReference("incus", "images", "ubuntu/24.04"));

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(server.lastPullRequest()).isPresent();
        FakeIncusServer.PullRequest request = server.lastPullRequest().orElseThrow();
        assertThat(request.alias()).isEqualTo("ubuntu/24.04");
        assertThat(request.server()).isEqualTo("https://images.linuxcontainers.org");
        assertThat(request.protocol()).isEqualTo("simplestreams");
    }

    @Test
    void pullRejectsReferenceOnOwnRemote() {
        assertThatThrownBy(() -> provider.pull(new ImageReference("incus", "local", "ubuntu/24.04")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pullRejectsUnknownRemote() {
        assertThatThrownBy(() -> provider.pull(new ImageReference("incus", "no-such-remote", "ubuntu/24.04")))
                .isInstanceOf(UnsupportedCapabilityException.class);
    }

    @Test
    void downloadStreamsExportedBytesWithChecksum() throws Exception {
        try (ImageDownload download = provider.download(new ImageReference("incus", "local", "fp-ubuntu-2404"))) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            download.stream().transferTo(out);

            assertThat(out.toString(StandardCharsets.UTF_8)).isEqualTo("fake-export-bytes-for-fp-ubuntu-2404");
            assertThat(download.checksum()).contains("fp-ubuntu-2404");
            assertThat(download.checksumAlgorithm()).contains("sha256");
        }
    }

    @Test
    void downloadResolvesAlias() throws Exception {
        try (ImageDownload download = provider.download(new ImageReference("incus", "local", "ubuntu/24.04"))) {
            assertThat(download.stream().readAllBytes()).isNotEmpty();
        }
    }

    @Test
    void downloadThrowsForUnknownImage() {
        assertThatThrownBy(() -> provider.download(new ImageReference("incus", "local", "missing")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void importFromInputStreamStreamsUploadAndReturnsImage() {
        byte[] payload = "fake-image-bytes".getBytes(StandardCharsets.UTF_8);
        InputStreamImageSource source = new InputStreamImageSource(new ByteArrayInputStream(payload), payload.length);

        ImageImportOperation operation = provider.importImage(source);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(operation.result()).isPresent();
        assertThat(operation.bytesTransferred()).hasValue(payload.length);
        assertThat(server.lastImportSize()).isEqualTo(payload.length);
    }
}
