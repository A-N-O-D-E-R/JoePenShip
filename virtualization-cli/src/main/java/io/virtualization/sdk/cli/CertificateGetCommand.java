package io.virtualization.sdk.cli;

import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a certificate by id.")
final class CertificateGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Certificate id.")
    String id;

    @Override
    public Integer call() {
        outputWriter().write(new CliResult.CertificateView(certificateManager().get(new CertificateId(id))), out());
        return ExitCodes.OK;
    }
}
