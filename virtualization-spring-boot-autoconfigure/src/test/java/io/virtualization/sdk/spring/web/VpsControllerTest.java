package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.spring.web.support.FakeImageProvider;
import io.virtualization.sdk.spring.web.support.FakeVirtualizationProvider;
import io.virtualization.sdk.vps.DefaultVpsManager;
import io.virtualization.sdk.vps.DefaultVpsProvisioner;
import io.virtualization.sdk.vps.InMemoryVpsRepository;
import io.virtualization.sdk.vps.InvalidVpsStateException;
import io.virtualization.sdk.vps.VpsManager;
import io.virtualization.sdk.vps.VpsState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VpsControllerTest {

    private static final CreateVpsRequestBody.ImageRequestBody IMAGE =
            new CreateVpsRequestBody.ImageRequestBody("fake", "images", "ubuntu/24.04");

    private VpsController controller;

    @BeforeEach
    void setUp() {
        VpsManager manager = new DefaultVpsManager(
                new InMemoryVpsRepository(), new DefaultVpsProvisioner(new FakeVirtualizationProvider(), new FakeImageProvider()));
        controller = new VpsController(manager);
    }

    private CreateVpsRequestBody createRequest() {
        return new CreateVpsRequestBody(
                "web-01", null, IMAGE, 2, 2048L, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void createReconcilesToReadyAndIsGettable() {
        CreateVpsView created = controller.create(createRequest());

        assertThat(created.operation().status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(created.vpsId()).isNotBlank();

        VpsView vps = controller.get(created.vpsId());
        assertThat(vps.state()).isEqualTo(VpsState.READY);
        assertThat(vps.name()).isEqualTo("web-01");
        assertThat(vps.cpuCores()).isEqualTo(2);
        assertThat(vps.memoryMb()).isEqualTo(2048L);
    }

    @Test
    void listIncludesCreatedVps() {
        CreateVpsView created = controller.create(createRequest());

        assertThat(controller.list()).extracting(VpsView::id).contains(created.vpsId());
    }

    @Test
    void missingNameThrows() {
        var request = new CreateVpsRequestBody(
                null, null, IMAGE, null, null, null, null, null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> controller.create(request)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void lifecycleTransitionsReflectInGet() {
        String id = controller.create(createRequest()).vpsId();

        assertThat(controller.stop(id).status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(controller.get(id).state()).isEqualTo(VpsState.STOPPED);

        assertThat(controller.start(id).status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(controller.get(id).state()).isEqualTo(VpsState.RUNNING);

        assertThat(controller.destroy(id).status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(controller.get(id).state()).isEqualTo(VpsState.DESTROYED);
    }

    @Test
    void invalidTransitionThrowsInvalidVpsState() {
        String id = controller.create(createRequest()).vpsId();

        assertThatThrownBy(() -> controller.start(id)).isInstanceOf(InvalidVpsStateException.class);
    }

    @Test
    void rebuildUpdatesImageAndReturnsToReady() {
        String id = controller.create(createRequest()).vpsId();
        var rebuildRequest = new RebuildVpsRequestBody(new CreateVpsRequestBody.ImageRequestBody("fake", "images", "ubuntu/24.04"));

        CreateVpsView rebuilt = controller.rebuild(id, rebuildRequest);

        assertThat(rebuilt.operation().status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(controller.get(id).state()).isEqualTo(VpsState.READY);
    }
}
