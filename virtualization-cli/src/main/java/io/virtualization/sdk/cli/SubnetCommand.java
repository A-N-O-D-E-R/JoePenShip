package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "subnet",
        description = "Manage CIDR subnets within a network.",
        subcommands = {SubnetListCommand.class, SubnetCreateCommand.class, SubnetGetCommand.class, SubnetDeleteCommand.class})
final class SubnetCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
