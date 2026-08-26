package io.virtualization.sdk.deployment;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class VpsDeploymentTargetTest {

    private static final Path DIR = Path.of("/etc/ssl/mock");

    @Test
    void rejectsNullVpsIdentifier() {
        assertThatNullPointerException().isThrownBy(() -> new VpsDeploymentTarget(null, DIR, ReverseProxy.NGINX));
    }

    @Test
    void rejectsBlankVpsIdentifier() {
        assertThatIllegalArgumentException().isThrownBy(() -> new VpsDeploymentTarget(" ", DIR, ReverseProxy.NGINX));
    }

    @Test
    void rejectsNullCertificateDirectory() {
        assertThatNullPointerException().isThrownBy(() -> new VpsDeploymentTarget("web-01", null, ReverseProxy.NGINX));
    }

    @Test
    void rejectsNullReverseProxy() {
        assertThatNullPointerException().isThrownBy(() -> new VpsDeploymentTarget("web-01", DIR, null));
    }
}
