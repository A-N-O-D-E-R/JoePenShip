package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "vm",
        description = "Manage virtual machines.",
        subcommands = {
            VmListCommand.class,
            VmGetCommand.class,
            VmStartCommand.class,
            VmStopCommand.class,
            VmRebootCommand.class,
            VmShutdownCommand.class,
            VmDestroyCommand.class
        })
final class VmCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
