package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import io.virtualization.sdk.core.image.WorkloadType;
import io.virtualization.sdk.spring.web.support.FakeVirtualizationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkloadControllerTest {

    private FakeVirtualizationProvider fakeProvider;
    private WorkloadController controller;

    @BeforeEach
    void setUp() {
        fakeProvider = new FakeVirtualizationProvider();
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of("fake", fakeProvider)), new ImageProviderRegistry(Map.of()));
        controller = new WorkloadController(client);
    }

    @Test
    void createSucceedsAndReturnsWorkloadId() {
        var request = new CreateWorkloadRequestBody(
                "fake", "ubuntu-test", WorkloadType.CONTAINER,
                new CreateWorkloadRequestBody.ImageRequestBody("images", "ubuntu/24.04"), 2, 2048L, null, null, null,
                null);

        CreateWorkloadView result = controller.create(request);

        assertThat(result.operation().status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(result.workloadId()).isEqualTo("ubuntu-test");
    }

    @Test
    void createFailureIsReportedInOperationView() {
        fakeProvider.nextOperationFails();
        var request = new CreateWorkloadRequestBody(
                "fake", "ubuntu-test", WorkloadType.CONTAINER,
                new CreateWorkloadRequestBody.ImageRequestBody("images", "ubuntu/24.04"), null, null, null, null, null,
                null);

        CreateWorkloadView result = controller.create(request);

        assertThat(result.operation().status()).isEqualTo(OperationStatus.FAILED);
        assertThat(result.workloadId()).isNull();
    }

    @Test
    void missingRequiredFieldThrows() {
        var request = new CreateWorkloadRequestBody(
                "fake", null, WorkloadType.CONTAINER, new CreateWorkloadRequestBody.ImageRequestBody("images", "ubuntu/24.04"),
                null, null, null, null, null, null);

        assertThatThrownBy(() -> controller.create(request)).isInstanceOf(NullPointerException.class);
    }
}
