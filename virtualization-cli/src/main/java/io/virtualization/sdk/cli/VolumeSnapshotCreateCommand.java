package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create a snapshot of a volume.")
final class VolumeSnapshotCreateCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<snapshot-name>", description = "Snapshot name.")
    String name;

    @Option(names = "--volume", required = true, description = "Volume name.")
    String volume;

    @Option(names = "--pool", required = true, description = "Storage pool name.")
    String pool;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_VOLUMES_SNAPSHOTS_CREATE);
        outputWriter().write(new CliResult.VolumeSnapshotView(storageProvider().createSnapshot(pool, volume, name)), out());
        return ExitCodes.OK;
    }
}
