package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List networks.")
final class NetworkListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_LIST);
        outputWriter().write(new CliResult.NetworkList(networkManager().list()), out());
        return ExitCodes.OK;
    }
}
