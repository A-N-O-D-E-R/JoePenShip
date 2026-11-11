package io.virtualization.sdk.proxmox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;
import io.virtualization.sdk.proxmox.client.ProxmoxApiClient;
import io.virtualization.sdk.proxmox.support.FakeProxmoxServer;
import io.virtualization.sdk.storage.Volume;
import io.virtualization.sdk.storage.VolumeContentType;
import io.virtualization.sdk.storage.VolumeSpec;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProxmoxStorageProviderTest {

    private FakeProxmoxServer server;
    private ProxmoxStorageProvider storage;
    private ProxmoxProvider provider;

    @BeforeEach
    void start() {
        server = new FakeProxmoxServer("PVEAPIToken=root@pam!sdk=token-secret").start();
        server.addVm(100, "pve1", "web-1", "running", 2, 2_147_483_648L);
        server.addVm(101, "pve1", "web-2", "running", 2, 2_147_483_648L);
        var client = new ProxmoxApiClient(ProxmoxClientConfig.of(server.uri(), new ProxmoxCredentials("root@pam!sdk", "token-secret")));
        storage = new ProxmoxStorageProvider(client, null, Duration.ofMillis(10));
        provider = new ProxmoxProvider(client, Duration.ofMillis(10));
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void volumesAreOwnedByTheReservedVmid() {
        Volume v = storage.createVolume("local", new VolumeSpec("data-1", VolumeContentType.BLOCK, "10G", null));
        assertThat(v.config()).containsEntry("volid", "local:999999999/vm-999999999-data-1.raw");
        assertThat(v.size()).isEqualTo("10G");
        Volume rbd = storage.createVolume("ceph", new VolumeSpec("data-2", VolumeContentType.BLOCK, "5GB", null));
        assertThat(rbd.config()).containsEntry("volid", "ceph:vm-999999999-data-2"); // block storage: no file extension

        assertThat(storage.listVolumes("local")).extracting(Volume::name).containsExactly("data-1");
        assertThat(storage.getStoragePool("ceph").driver()).isEqualTo("rbd");
        assertThatThrownBy(() -> storage.createVolume("local", new VolumeSpec("data-1", VolumeContentType.BLOCK, "1G", null)))
                .isInstanceOf(OperationException.class);
        assertThatThrownBy(() -> storage.createVolume("local", new VolumeSpec("Bad_Name", VolumeContentType.BLOCK, "1G", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.createVolume("local", new VolumeSpec("x", VolumeContentType.BLOCK, "1.5T", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.getVolume("nope", "data-1")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> storage.createSnapshot("local", "data-1", "s1")).isInstanceOf(UnsupportedCapabilityException.class);

        storage.deleteVolume("ceph", "data-2");
        assertThat(server.volumes()).containsOnlyKeys("local:999999999/vm-999999999-data-1.raw");
    }

    @Test
    void attachResizeDetachDelete() {
        String volid = storage.createVolume("local", new VolumeSpec("db", VolumeContentType.BLOCK, "10", null)).config().get("volid");
        assertThatThrownBy(() -> provider.attachVolume("100", volid, "scsi0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.resizeVolume("local", "db", "20G"))
                .isInstanceOf(OperationException.class).hasMessageContaining("only while attached");

        assertThat(provider.attachVolume("100", volid, "scsi1").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(server.config(100)).containsEntry("scsi1", volid + ",size=10G");
        assertThat(provider.attachVolume("100", volid, "scsi1").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        // One VM at a time, one volume per slot.
        assertThat(provider.attachVolume("101", volid, "scsi1").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThatThrownBy(() -> storage.deleteVolume("local", "db")).isInstanceOf(OperationException.class);

        assertThat(storage.resizeVolume("local", "db", "20G").size()).isEqualTo("20G");
        assertThat(server.config(100).get("scsi1")).endsWith("size=20G");

        assertThat(provider.detachVolume("100", volid).await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(server.config(100)).doesNotContainKey("scsi1").doesNotContainKey("unused0");
        assertThat(provider.detachVolume("100", volid).await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED); // idempotent
        assertThat(provider.attachVolume("101", volid, "scsi2").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(provider.detachVolume("101", volid).await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);

        storage.deleteVolume("local", "db");
        assertThat(server.volumes()).isEmpty();
    }

    @Test
    void createFromImageNeverUsesTheVolumeOwnerVmid() {
        server.addTemplate(9000, "pve1", "debian-13", "10G");
        server.addVm(999_999_998, "pve1", "squatter", "stopped", 1, 1_073_741_824L); // nextid would now be the owner VMID

        var op = provider.createFromImage(new ImageReference("proxmox", "9000"),
                WorkloadSpec.builder("i-0owner", WorkloadType.VIRTUAL_MACHINE).build());

        assertThat(op.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(op.error()).get().extracting(Throwable::getMessage).asString().contains("reserved for standalone volumes");
        assertThat(server.vmidByName("i-0owner")).isNull();
    }
}
