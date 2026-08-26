package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.VirtualizationProvider;
import picocli.CommandLine.Command;

@Command(name = "stop", description = "Stop a virtual machine.")
final class VmStopCommand extends AbstractLifecycleCommand {

    @Override
    protected Operation invoke(VirtualizationProvider provider, String id) {
        return provider.stop(id);
    }
}
