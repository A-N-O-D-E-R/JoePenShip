package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import io.virtualization.sdk.spring.web.support.FakeImageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageControllerTest {

    private FakeImageProvider fakeImages;
    private ImageController controller;

    @BeforeEach
    void setUp() {
        fakeImages = new FakeImageProvider();
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of()), new ImageProviderRegistry(Map.of("fake", fakeImages)));
        controller = new ImageController(client);
    }

    @Test
    void listReturnsConfiguredImage() {
        List<Image> images = controller.list("fake");

        assertThat(images).extracting(Image::name).containsExactly("ubuntu/24.04");
    }

    @Test
    void searchFiltersByDistribution() {
        assertThat(controller.search("fake", null, "ubuntu", null, null, null, null, null)).hasSize(1);
        assertThat(controller.search("fake", null, "debian", null, null, null, null, null)).isEmpty();
    }

    @Test
    void getResolvesKnownReference() {
        Image image = controller.get("fake", "images:ubuntu/24.04");

        assertThat(image.name()).isEqualTo("ubuntu/24.04");
    }

    @Test
    void getUnknownReferenceThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("fake", "images:missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void downloadStreamsBytesWithHeaders() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.download("fake", "images:ubuntu/24.04", response);

        assertThat(response.getContentAsString()).isEqualTo("fake-image-bytes");
        assertThat(response.getContentType()).isEqualTo("application/octet-stream");
        assertThat(response.getHeader("Content-Disposition")).contains("images_ubuntu_24.04");
    }

    @Test
    void pullSucceedsAndReturnsOperationView() {
        OperationView result = controller.pull(new PullRequestBody("fake", "images", "ubuntu/24.04"));

        assertThat(result.status()).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void pullFailureIsReportedInOperationView() {
        fakeImages.nextOperationFails();

        OperationView result = controller.pull(new PullRequestBody("fake", "images", "ubuntu/24.04"));

        assertThat(result.status()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.error()).isEqualTo("simulated failure");
    }

    @Test
    void importImageStreamsRequestBodyAndReturnsImportedImage() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        byte[] payload = "fake-upload-bytes".getBytes(StandardCharsets.UTF_8);
        request.setContent(payload);

        ImageImportView result = controller.importImage("fake", request);

        assertThat(result.operation().status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(result.image()).isNotNull();
        assertThat(result.image().name()).isEqualTo("ubuntu/24.04");
    }
}
