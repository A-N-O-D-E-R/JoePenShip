package io.virtualization.sdk.cli;

import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "renew", description = "Renew a certificate.")
final class CertificateRenewCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Certificate id.")
    String id;

    @Override
    public Integer call() {
        outputWriter().write(new CliResult.CertificateView(certificateManager().renew(new CertificateId(id))), out());
        return ExitCodes.OK;
    }
}
