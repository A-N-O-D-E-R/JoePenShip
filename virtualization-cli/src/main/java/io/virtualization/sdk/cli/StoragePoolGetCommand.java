package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a storage pool by name.")
final class StoragePoolGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "Storage pool name.")
    String name;

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_POOLS_GET);
        outputWriter().write(new CliResult.StoragePoolView(storageProvider().getStoragePool(name)), out());
        return ExitCodes.OK;
    }
}
