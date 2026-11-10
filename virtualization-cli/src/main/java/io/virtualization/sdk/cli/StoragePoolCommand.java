package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "storage-pool",
        description = "List and inspect storage pools (read-only — pool lifecycle is an infrastructure-admin operation).",
        subcommands = {StoragePoolListCommand.class, StoragePoolGetCommand.class})
final class StoragePoolCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
