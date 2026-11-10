package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "shutdown", description = "Gracefully shut down a virtual machine.")
final class VmShutdownCommand extends AbstractLifecycleCommand {

    @Override
    protected Operation invoke(VirtualizationProvider provider, String id) {
        return provider.shutdown(id);
    }

    @Override
    protected String permission() {
        return Permissions.VM_SHUTDOWN;
    }
}
