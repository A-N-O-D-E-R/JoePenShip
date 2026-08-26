package io.virtualization.sdk.deployment;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateMaterial;
import io.virtualization.sdk.certificate.CertificateStore;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Deploys certificate material to a local directory — a real, shippable reference implementation
 * of {@link CertificateDeployer} for local development and testing, not a real remote (SSH-based)
 * deployer yet.
 *
 * <p>Each file (`cert.pem`/`privkey.pem`/`chain.pem`) is written to a sibling {@code .tmp} path
 * first, then atomically moved into place — spec §29's "upload new material, validate, atomic
 * switch, reload service, keep old version temporarily": the temp-write step is the upload, {@link
 * CertificateMaterial}'s own non-blank invariant (enforced at construction) is the validation, the
 * atomic move is the switch, and any file already at the target path is preserved as {@code
 * .previous} rather than deleted — a failure at any point before the atomic move leaves the
 * previously-deployed, still-valid files completely untouched (spec §28).
 */
public final class LocalFilesystemCertificateDeployer implements CertificateDeployer {

    private static final Logger log = LoggerFactory.getLogger(LocalFilesystemCertificateDeployer.class);

    private final CertificateStore certificateStore;

    public LocalFilesystemCertificateDeployer(CertificateStore certificateStore) {
        this.certificateStore = Objects.requireNonNull(certificateStore, "certificateStore must not be null");
    }

    @Override
    public void deploy(Certificate certificate, DeploymentTarget target) {
        Objects.requireNonNull(certificate, "certificate must not be null");
        if (!(target instanceof VpsDeploymentTarget vpsTarget)) {
            throw new UnsupportedCapabilityException(
                    "LocalFilesystemCertificateDeployer only supports VpsDeploymentTarget, got " + target.getClass().getSimpleName());
        }
        CertificateMaterial material = certificateStore.load(certificate.id())
                .orElseThrow(() -> new ResourceNotFoundException("No certificate material stored for '" + certificate.id().value() + "'"));

        Path dir = vpsTarget.certificateDirectory();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new OperationException("Failed to create certificate directory '" + dir + "'", e);
        }
        writeAtomically(dir.resolve("cert.pem"), material.certificate());
        writeAtomically(dir.resolve("privkey.pem"), material.privateKey());
        writeAtomically(dir.resolve("chain.pem"), material.chain());

        reload(vpsTarget);
    }

    private static void writeAtomically(Path target, String content) {
        try {
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, content);
            if (Files.exists(target)) {
                Path previous = target.resolveSibling(target.getFileName() + ".previous");
                Files.move(target, previous, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new OperationException("Failed to deploy certificate file '" + target + "'", e);
        }
    }

    private void reload(VpsDeploymentTarget target) {
        if (target.reverseProxy() == ReverseProxy.NONE) {
            return;
        }
        // ponytail: log-only — no real process to reload for a local-filesystem deployer. A real
        // SSH-based deployer would run a restricted, known reload command here (never arbitrary).
        log.info("Reload triggered for {} on '{}' (no real process managed)", target.reverseProxy(), target.vpsIdentifier());
    }
}
