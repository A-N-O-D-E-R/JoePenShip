package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "firewall-rule",
        description = "Manage firewall rules within a network (modeled only — no provider enforces them yet).",
        subcommands = {FirewallRuleListCommand.class, FirewallRuleCreateCommand.class, FirewallRuleDeleteCommand.class})
final class FirewallRuleCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
