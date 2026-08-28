package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.storage.VolumeContentType;
import io.virtualization.sdk.storage.VolumeSpec;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.Map;

@Command(name = "create", description = "Create a custom volume in a pool.")
final class VolumeCreateCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "Volume name.")
    String name;

    @Option(names = "--pool", required = true, description = "Storage pool name.")
    String pool;

    @Option(names = "--content-type", description = "Volume content type: ${COMPLETION-CANDIDATES}.")
    VolumeContentType contentType;

    @Option(names = "--size", description = "Volume size, e.g. 10GB (default: pool default).")
    String size;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_VOLUMES_CREATE);
        outputWriter().write(new CliResult.VolumeView(
                storageProvider().createVolume(pool, new VolumeSpec(name, contentType, size, Map.of()))), out());
        return ExitCodes.OK;
    }
}
