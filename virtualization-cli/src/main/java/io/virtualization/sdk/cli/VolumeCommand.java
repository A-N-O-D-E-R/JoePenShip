package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "volume",
        description = "Manage storage volumes (always custom volumes) within a pool.",
        subcommands = {
            VolumeListCommand.class, VolumeGetCommand.class, VolumeCreateCommand.class, VolumeDeleteCommand.class,
            VolumeResizeCommand.class, VolumeSnapshotCommand.class
        })
final class VolumeCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
