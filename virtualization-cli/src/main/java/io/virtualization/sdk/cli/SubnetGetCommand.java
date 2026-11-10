package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.SubnetId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a subnet by id.")
final class SubnetGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Subnet id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_SUBNETS_GET);
        outputWriter().write(new CliResult.SubnetView(subnetManager().get(new SubnetId(id))), out());
        return ExitCodes.OK;
    }
}
