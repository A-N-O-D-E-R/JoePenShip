package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.NetworkId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create a subnet within a network.")
final class SubnetCreateCommand extends AbstractCliCommand {

    @Option(names = "--network", required = true, description = "Owning network id.")
    String networkId;

    @Option(names = "--cidr", required = true, description = "CIDR block, e.g. 10.0.0.0/24.")
    String cidr;

    @Option(names = "--gateway", description = "Reserved gateway address, excluded from allocation.")
    String gateway;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_SUBNETS_CREATE);
        outputWriter().write(new CliResult.SubnetView(subnetManager().create(new NetworkId(networkId), cidr, gateway)), out());
        return ExitCodes.OK;
    }
}
