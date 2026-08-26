package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsManager;
import picocli.CommandLine.Command;

@Command(name = "shutdown", description = "Gracefully shut down a VPS.")
final class VpsShutdownCommand extends AbstractVpsLifecycleCommand {

    @Override
    protected Operation invoke(VpsManager manager, VpsId id) {
        return manager.shutdown(id);
    }
}
