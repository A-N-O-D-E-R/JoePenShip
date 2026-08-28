package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

import java.util.List;

@Command(name = "list", description = "List virtual machines.")
final class VmListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.VM_LIST);
        List<VirtualMachine> vms = provider().listVirtualMachines();
        outputWriter().write(new CliResult.VmList(vms), out());
        return ExitCodes.OK;
    }
}
