package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.VirtualizationProvider;
import picocli.CommandLine.Command;

@Command(name = "start", description = "Start a virtual machine.")
final class VmStartCommand extends AbstractLifecycleCommand {

    @Override
    protected Operation invoke(VirtualizationProvider provider, String id) {
        return provider.start(id);
    }
}
