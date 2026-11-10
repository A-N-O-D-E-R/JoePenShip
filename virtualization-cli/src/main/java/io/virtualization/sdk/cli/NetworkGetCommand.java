package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.NetworkId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a network by id.")
final class NetworkGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Network id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_GET);
        outputWriter().write(new CliResult.NetworkView(networkManager().get(new NetworkId(id))), out());
        return ExitCodes.OK;
    }
}
