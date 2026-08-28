package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.NetworkId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List firewall rules in a network.")
final class FirewallRuleListCommand extends AbstractCliCommand {

    @Option(names = "--network", required = true, description = "Owning network id.")
    String networkId;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_FIREWALL_RULES_LIST);
        outputWriter().write(new CliResult.FirewallRuleList(firewallRuleManager().list(new NetworkId(networkId))), out());
        return ExitCodes.OK;
    }
}
