package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "snapshot",
        description = "Manage volume snapshots.",
        subcommands = {
            VolumeSnapshotListCommand.class, VolumeSnapshotCreateCommand.class, VolumeSnapshotRestoreCommand.class,
            VolumeSnapshotDeleteCommand.class
        })
final class VolumeSnapshotCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
