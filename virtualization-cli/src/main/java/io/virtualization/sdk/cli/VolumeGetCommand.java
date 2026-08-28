package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a volume by name.")
final class VolumeGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "Volume name.")
    String name;

    @Option(names = "--pool", required = true, description = "Storage pool name.")
    String pool;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_VOLUMES_GET);
        outputWriter().write(new CliResult.VolumeView(storageProvider().getVolume(pool, name)), out());
        return ExitCodes.OK;
    }
}
