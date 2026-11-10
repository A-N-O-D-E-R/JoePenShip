package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a volume.")
final class VolumeDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "Volume name.")
    String name;

    @Option(names = "--pool", required = true, description = "Storage pool name.")
    String pool;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_VOLUMES_DELETE);
        storageProvider().deleteVolume(pool, name);
        outputWriter().write(new CliResult.Ack(name, "Volume deleted."), out());
        return ExitCodes.OK;
    }
}
