package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a snapshot.")
final class VolumeSnapshotDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<snapshot-name>", description = "Snapshot name.")
    String name;

    @Option(names = "--volume", required = true, description = "Volume name.")
    String volume;

    @Option(names = "--pool", required = true, description = "Storage pool name.")
    String pool;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_VOLUMES_SNAPSHOTS_DELETE);
        storageProvider().deleteSnapshot(pool, volume, name);
        outputWriter().write(new CliResult.Ack(name, "Snapshot deleted."), out());
        return ExitCodes.OK;
    }
}
