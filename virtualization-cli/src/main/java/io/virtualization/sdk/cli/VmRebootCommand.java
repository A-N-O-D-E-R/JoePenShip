package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.VirtualizationProvider;
import picocli.CommandLine.Command;

@Command(name = "reboot", description = "Reboot a virtual machine.")
final class VmRebootCommand extends AbstractLifecycleCommand {

    @Override
    protected Operation invoke(VirtualizationProvider provider, String id) {
        return provider.reboot(id);
    }
}
