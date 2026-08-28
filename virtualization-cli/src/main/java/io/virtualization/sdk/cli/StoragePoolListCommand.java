package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List storage pools.")
final class StoragePoolListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.STORAGE_POOLS_LIST);
        outputWriter().write(new CliResult.StoragePoolList(storageProvider().listStoragePools()), out());
        return ExitCodes.OK;
    }
}
