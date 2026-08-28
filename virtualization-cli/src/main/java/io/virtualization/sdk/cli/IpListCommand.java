package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.SubnetId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List allocations in a subnet.")
final class IpListCommand extends AbstractCliCommand {

    @Option(names = "--subnet", required = true, description = "Subnet id.")
    String subnetId;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_IPS_LIST);
        outputWriter().write(new CliResult.IpAllocationList(ipamService().list(new SubnetId(subnetId))), out());
        return ExitCodes.OK;
    }
}
