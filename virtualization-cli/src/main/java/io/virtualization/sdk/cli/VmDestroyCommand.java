package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.VirtualizationProvider;
import picocli.CommandLine.Command;

@Command(name = "destroy", description = "Destroy (delete) a virtual machine.")
final class VmDestroyCommand extends AbstractLifecycleCommand {

    @Override
    protected Operation invoke(VirtualizationProvider provider, String id) {
        return provider.destroy(id);
    }
}
