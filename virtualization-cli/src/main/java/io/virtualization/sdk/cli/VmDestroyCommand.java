package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "destroy", description = "Destroy (delete) a virtual machine.")
final class VmDestroyCommand extends AbstractLifecycleCommand {

    @Override
    protected Operation invoke(VirtualizationProvider provider, String id) {
        return provider.destroy(id);
    }

    @Override
    protected String permission() {
        return Permissions.VM_DESTROY;
    }
}
