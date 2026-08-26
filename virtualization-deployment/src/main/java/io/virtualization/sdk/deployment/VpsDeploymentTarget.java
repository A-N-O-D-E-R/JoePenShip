package io.virtualization.sdk.deployment;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A VPS to deploy certificate material onto. {@code certificateDirectory} is where {@code
 * cert.pem}/{@code privkey.pem}/{@code chain.pem} get written — a local path for {@link
 * LocalFilesystemCertificateDeployer}; a real SSH-based deployer (not implemented yet) would
 * interpret it as a remote path instead.
 *
 * @param vpsIdentifier a caller-meaningful label for the target VPS (not validated against {@code
 *                       virtualization-vps} — this module has no dependency on it)
 */
public record VpsDeploymentTarget(String vpsIdentifier, Path certificateDirectory, ReverseProxy reverseProxy) implements DeploymentTarget {

    public VpsDeploymentTarget {
        Objects.requireNonNull(vpsIdentifier, "vpsIdentifier must not be null");
        if (vpsIdentifier.isBlank()) {
            throw new IllegalArgumentException("vpsIdentifier must not be blank");
        }
        Objects.requireNonNull(certificateDirectory, "certificateDirectory must not be null");
        Objects.requireNonNull(reverseProxy, "reverseProxy must not be null");
    }
}
