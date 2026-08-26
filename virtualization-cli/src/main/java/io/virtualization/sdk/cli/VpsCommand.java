package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "vps",
        description = "Manage VPSs: provider-neutral image + compute + storage + network + lifecycle.",
        subcommands = {
            VpsCreateCommand.class,
            VpsListCommand.class,
            VpsGetCommand.class,
            VpsStartCommand.class,
            VpsStopCommand.class,
            VpsRestartCommand.class,
            VpsShutdownCommand.class,
            VpsDestroyCommand.class,
            VpsRebuildCommand.class
        })
final class VpsCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
