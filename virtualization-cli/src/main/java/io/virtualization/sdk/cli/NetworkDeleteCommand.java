package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.NetworkId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a network.")
final class NetworkDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Network id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_DELETE);
        networkManager().delete(new NetworkId(id));
        outputWriter().write(new CliResult.Ack(id, "Network deleted."), out());
        return ExitCodes.OK;
    }
}
