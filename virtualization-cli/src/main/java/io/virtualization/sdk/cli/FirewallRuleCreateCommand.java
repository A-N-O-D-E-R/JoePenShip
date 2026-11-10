package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.FirewallAction;
import io.virtualization.sdk.network.FirewallDirection;
import io.virtualization.sdk.network.FirewallProtocol;
import io.virtualization.sdk.network.NetworkId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create a firewall rule within a network.")
final class FirewallRuleCreateCommand extends AbstractCliCommand {

    @Option(names = "--network", required = true, description = "Owning network id.")
    String networkId;

    @Option(names = "--direction", required = true, description = "Rule direction: ${COMPLETION-CANDIDATES}.")
    FirewallDirection direction;

    @Option(names = "--protocol", required = true, description = "Rule protocol: ${COMPLETION-CANDIDATES}.")
    FirewallProtocol protocol;

    @Option(names = "--port-range", description = "Port range as <start>-<end>, e.g. 80-443. Omit to match every port.")
    String portRange;

    @Option(names = "--cidr", required = true, description = "Source/destination CIDR to match.")
    String cidr;

    @Option(names = "--action", required = true, description = "Rule action: ${COMPLETION-CANDIDATES}.")
    FirewallAction action;

    @Option(names = "--priority", defaultValue = "0", description = "Rule priority (default: 0).")
    int priority;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_FIREWALL_RULES_CREATE);
        Integer portStart = null;
        Integer portEnd = null;
        if (portRange != null) {
            String[] parts = portRange.split("-", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("--port-range must be '<start>-<end>', got '" + portRange + "'");
            }
            portStart = Integer.parseInt(parts[0]);
            portEnd = Integer.parseInt(parts[1]);
        }
        outputWriter().write(new CliResult.FirewallRuleView(firewallRuleManager().create(
                new NetworkId(networkId), direction, protocol, portStart, portEnd, cidr, action, priority)), out());
        return ExitCodes.OK;
    }
}
