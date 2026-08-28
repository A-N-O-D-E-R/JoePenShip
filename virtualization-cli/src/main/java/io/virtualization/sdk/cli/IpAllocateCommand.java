package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.SubnetId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "allocate", description = "Allocate the next free address from a subnet.")
final class IpAllocateCommand extends AbstractCliCommand {

    @Option(names = "--subnet", required = true, description = "Subnet id to allocate from.")
    String subnetId;

    @Option(names = "--owner", defaultValue = "unspecified", description = "Free-form owner reference (default: unspecified).")
    String owner;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_IPS_ALLOCATE);
        outputWriter().write(new CliResult.IpAllocationView(ipamService().allocate(new SubnetId(subnetId), owner)), out());
        return ExitCodes.OK;
    }
}
