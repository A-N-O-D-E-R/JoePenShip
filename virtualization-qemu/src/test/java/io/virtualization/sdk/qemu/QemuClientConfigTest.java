package io.virtualization.sdk.qemu;

import io.virtualization.sdk.qemu.qmp.TcpEndpoint;
import io.virtualization.sdk.qemu.qmp.UnixSocketEndpoint;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class QemuClientConfigTest {

    @Test
    void unixSocketFactoryBuildsUnixEndpoint() {
        QemuClientConfig config = QemuClientConfig.unixSocket("local-vm", Path.of("/run/qemu/myvm.qmp"));

        assertThat(config.vmId()).isEqualTo("local-vm");
        assertThat(config.endpoint()).isInstanceOf(UnixSocketEndpoint.class);
    }

    @Test
    void tcpFactoryBuildsTcpEndpoint() {
        QemuClientConfig config = QemuClientConfig.tcp("local-vm", "localhost", 4444);

        assertThat(config.endpoint()).isEqualTo(new TcpEndpoint("localhost", 4444));
    }

    @Test
    void rejectsBlankVmId() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> QemuClientConfig.unixSocket(" ", Path.of("/run/qemu/myvm.qmp")));
    }

    @Test
    void rejectsNullEndpoint() {
        assertThatNullPointerException().isThrownBy(() -> new QemuClientConfig(
                "vm-1", null, java.time.Duration.ofSeconds(1), java.time.Duration.ofSeconds(1)));
    }
}
