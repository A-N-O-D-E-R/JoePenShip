package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsManager;
import picocli.CommandLine.Command;

@Command(name = "stop", description = "Stop a VPS.")
final class VpsStopCommand extends AbstractVpsLifecycleCommand {

    @Override
    protected Operation invoke(VpsManager manager, VpsId id) {
        return manager.stop(id);
    }

    @Override
    protected String permission() {
        return Permissions.VPS_STOP;
    }
}
