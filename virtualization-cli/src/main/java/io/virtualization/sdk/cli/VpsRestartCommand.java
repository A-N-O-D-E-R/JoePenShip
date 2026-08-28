package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsManager;
import picocli.CommandLine.Command;

@Command(name = "restart", description = "Restart a VPS.")
final class VpsRestartCommand extends AbstractVpsLifecycleCommand {

    @Override
    protected Operation invoke(VpsManager manager, VpsId id) {
        return manager.restart(id);
    }

    @Override
    protected String permission() {
        return Permissions.VPS_RESTART;
    }
}
