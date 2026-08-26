package io.virtualization.sdk.cli;

import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "revoke", description = "Revoke a certificate.")
final class CertificateRevokeCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Certificate id.")
    String id;

    @Override
    public Integer call() {
        certificateManager().revoke(new CertificateId(id));
        outputWriter().write(new CliResult.Ack(id, "Certificate revoked."), out());
        return ExitCodes.OK;
    }
}
