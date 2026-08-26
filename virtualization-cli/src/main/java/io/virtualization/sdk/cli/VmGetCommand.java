package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.VirtualMachine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a virtual machine by id.")
final class VmGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Virtual machine id.")
    String id;

    @Override
    public Integer call() {
        VirtualMachine vm = provider().getVirtualMachine(id);
        outputWriter().write(new CliResult.Vm(vm), out());
        return ExitCodes.OK;
    }
}
