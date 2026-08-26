/**
 * Certificate deployment: {@link io.virtualization.sdk.deployment.CertificateDeployer}
 * implementations get issued {@link io.virtualization.sdk.certificate.CertificateMaterial} onto a
 * {@link io.virtualization.sdk.deployment.DeploymentTarget} — a controlled abstraction, never an
 * unrestricted remote-command API. {@link
 * io.virtualization.sdk.deployment.LocalFilesystemCertificateDeployer} is a real, shippable
 * reference implementation (writes to a local directory, atomic swap, keeps the previous version)
 * for local development and testing — not a real SSH-based deployer yet (a later phase). Pure
 * Java, no framework dependencies.
 */
package io.virtualization.sdk.deployment;
