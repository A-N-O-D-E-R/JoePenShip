package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List volumes in a pool.")
final class VolumeListCommand extends AbstractCliCommand {

    @Option(names = "--pool", required = true, description = "Storage pool name.")
    String pool;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_VOLUMES_LIST);
        outputWriter().write(new CliResult.VolumeList(storageProvider().listVolumes(pool)), out());
        return ExitCodes.OK;
    }
}
