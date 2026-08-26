package io.virtualization.sdk.deployment;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateStore;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;

/**
 * Gets a {@link Certificate}'s material onto a {@link DeploymentTarget}. Deliberately no {@code
 * CertificateMaterial} parameter — implementations pull it themselves from a {@link
 * CertificateStore} they're constructed with, so key material never has to travel through a
 * caller that only wants to trigger a deployment. Not an unrestricted remote-command API: an
 * implementation supports a fixed, known set of {@link DeploymentTarget} types and does exactly
 * one thing (write certificate files, optionally reload a named service) — no arbitrary command
 * execution.
 */
public interface CertificateDeployer {

    /**
     * @throws ResourceNotFoundException      if no material is stored for {@code certificate}
     * @throws UnsupportedCapabilityException if this deployer doesn't support {@code target}'s type
     */
    void deploy(Certificate certificate, DeploymentTarget target);
}
