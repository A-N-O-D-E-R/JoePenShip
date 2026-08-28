package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List snapshots of a volume.")
final class VolumeSnapshotListCommand extends AbstractCliCommand {

    @Option(names = "--volume", required = true, description = "Volume name.")
    String volume;

    @Option(names = "--pool", required = true, description = "Storage pool name.")
    String pool;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_VOLUMES_SNAPSHOTS_LIST);
        outputWriter().write(new CliResult.VolumeSnapshotList(storageProvider().listSnapshots(pool, volume)), out());
        return ExitCodes.OK;
    }
}
