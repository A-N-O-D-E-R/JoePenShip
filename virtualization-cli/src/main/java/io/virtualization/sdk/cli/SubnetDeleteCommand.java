package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.SubnetId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a subnet.")
final class SubnetDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Subnet id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_SUBNETS_DELETE);
        subnetManager().delete(new SubnetId(id));
        outputWriter().write(new CliResult.Ack(id, "Subnet deleted."), out());
        return ExitCodes.OK;
    }
}
