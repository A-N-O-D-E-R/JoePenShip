package io.virtualization.sdk.cli;

import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.certificate.CertificateMaterial;
import io.virtualization.sdk.cli.certificate.Passphrases;
import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "export", description = "Export certificate material to local files.")
final class CertificateExportCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Certificate id.")
    String id;

    @Option(names = "--cert", description = "Path to write the leaf certificate PEM to.")
    Path certPath;

    @Option(names = "--chain", description = "Path to write the intermediate chain PEM to.")
    Path chainPath;

    @Option(names = "--private-key", description = "Path to write the private key PEM to — requires --include-private-key and --yes.")
    Path privateKeyPath;

    @Option(names = "--include-private-key", description = "Acknowledge that --private-key will write secret key material to disk.")
    boolean includePrivateKey;

    @Option(names = "--yes", description = "Confirm writing private key material to disk.")
    boolean yes;

    @Option(
            names = "--passphrase",
            description = "Passphrase to decrypt certificate material with (prefer the VIRTUALIZATION_CERTIFICATE_PASSPHRASE "
                    + "environment variable — a flag value may land in shell history).")
    String passphrase;

    @Override
    public Integer call() {
        if (privateKeyPath != null && !(includePrivateKey && yes)) {
            throw new ParameterException(
                    spec.commandLine(),
                    "Refusing to write private key material: --private-key requires both --include-private-key and --yes.");
        }

        CertificateId certificateId = new CertificateId(id);
        certificateManager().get(certificateId); // ResourceNotFoundException if unknown, before touching any material
        CertificateMaterial material = certificateStore(Passphrases.resolve(passphrase)).load(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("No certificate material stored for id '" + id + "'"));

        boolean cert = writeIfRequested(certPath, material.certificate());
        boolean chain = writeIfRequested(chainPath, material.chain());
        boolean privateKey = false;
        if (privateKeyPath != null) {
            out().println("Warning: writing private key material to " + privateKeyPath);
            privateKey = writeIfRequested(privateKeyPath, material.privateKey());
        }

        outputWriter().write(new CliResult.CertificateExportResult(id, cert, chain, privateKey), out());
        return ExitCodes.OK;
    }

    private static boolean writeIfRequested(Path path, String content) {
        if (path == null) {
            return false;
        }
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write '" + path + "'", e);
        }
    }
}
