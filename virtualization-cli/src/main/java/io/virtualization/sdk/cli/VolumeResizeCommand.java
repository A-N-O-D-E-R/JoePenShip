package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "resize", description = "Resize a volume.")
final class VolumeResizeCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "Volume name.")
    String name;

    @Option(names = "--pool", required = true, description = "Storage pool name.")
    String pool;

    @Option(names = "--size", required = true, description = "New volume size, e.g. 20GB.")
    String size;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_VOLUMES_RESIZE);
        outputWriter().write(new CliResult.VolumeView(storageProvider().resizeVolume(pool, name, size)), out());
        return ExitCodes.OK;
    }
}
