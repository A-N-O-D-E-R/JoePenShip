package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List requested certificates.")
final class CertificateListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        outputWriter().write(new CliResult.CertificateList(certificateManager().list()), out());
        return ExitCodes.OK;
    }
}
