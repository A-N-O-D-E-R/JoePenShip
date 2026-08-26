package io.virtualization.sdk.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DomainModelTest {

    private static final ComputeResources RESOURCES = new ComputeResources(2, 2048);

    @Test
    void virtualMachineConstructsAndRejectsInvalidId() {
        VirtualMachine vm = new VirtualMachine("vm-1", "web", VirtualMachineState.RUNNING, RESOURCES);
        assertThat(vm.id()).isEqualTo("vm-1");
        assertThat(vm.state()).isEqualTo(VirtualMachineState.RUNNING);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new VirtualMachine("", "web", VirtualMachineState.RUNNING, RESOURCES));
        assertThatNullPointerException()
                .isThrownBy(() -> new VirtualMachine(null, "web", VirtualMachineState.RUNNING, RESOURCES));
    }

    @Test
    void containerConstructsAndRejectsInvalidId() {
        Container container = new Container("ct-1", "cache", VirtualMachineState.STOPPED, RESOURCES);
        assertThat(container.id()).isEqualTo("ct-1");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Container("", "cache", VirtualMachineState.STOPPED, RESOURCES));
    }

    @Test
    void nodeConstructsAndRejectsInvalidId() {
        Node node = new Node("node-1", "pve1");
        assertThat(node.name()).isEqualTo("pve1");

        assertThatIllegalArgumentException().isThrownBy(() -> new Node(" ", "pve1"));
    }

    @Test
    void clusterConstructsAndRejectsInvalidId() {
        Cluster cluster = new Cluster("cluster-1", "prod");
        assertThat(cluster.name()).isEqualTo("prod");

        assertThatIllegalArgumentException().isThrownBy(() -> new Cluster("", "prod"));
    }

    @Test
    void storageConstructsAndRejectsInvalidId() {
        Storage storage = new Storage("storage-1", "local-lvm");
        assertThat(storage.name()).isEqualTo("local-lvm");

        assertThatIllegalArgumentException().isThrownBy(() -> new Storage("", "local-lvm"));
    }

    @Test
    void networkConstructsAndRejectsInvalidId() {
        Network network = new Network("net-1", "vmbr0");
        assertThat(network.name()).isEqualTo("vmbr0");

        assertThatIllegalArgumentException().isThrownBy(() -> new Network("", "vmbr0"));
    }

    @Test
    void imageConstructsAndRejectsInvalidId() {
        Image image = new Image("image-1", "debian-13");
        assertThat(image.name()).isEqualTo("debian-13");

        assertThatIllegalArgumentException().isThrownBy(() -> new Image("", "debian-13"));
    }

    @Test
    void snapshotConstructsAndRejectsInvalidId() {
        Instant now = Instant.now();
        Snapshot snapshot = new Snapshot("snap-1", "before-upgrade", now);
        assertThat(snapshot.createdAt()).isEqualTo(now);

        assertThatIllegalArgumentException().isThrownBy(() -> new Snapshot("", "before-upgrade", now));
        assertThatNullPointerException().isThrownBy(() -> new Snapshot("snap-1", "before-upgrade", null));
    }
}
