package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.FirewallRuleId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a firewall rule.")
final class FirewallRuleDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Firewall rule id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_FIREWALL_RULES_DELETE);
        firewallRuleManager().delete(new FirewallRuleId(id));
        outputWriter().write(new CliResult.Ack(id, "Firewall rule deleted."), out());
        return ExitCodes.OK;
    }
}
