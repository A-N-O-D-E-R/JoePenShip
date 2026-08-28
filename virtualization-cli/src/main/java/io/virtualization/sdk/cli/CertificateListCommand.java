package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List requested certificates.")
final class CertificateListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.CERTIFICATE_LIST);
        outputWriter().write(new CliResult.CertificateList(certificateManager().list()), out());
        return ExitCodes.OK;
    }
}
