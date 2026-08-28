package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "route",
        description = "Manage virtual-host routes within a load balancer.",
        subcommands = {RouteListCommand.class, RouteCreateCommand.class, RouteGetCommand.class, RouteUpdateCommand.class, RouteDeleteCommand.class})
final class RouteCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
