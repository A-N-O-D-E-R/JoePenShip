package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.network.IpAllocationId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "release", description = "Release an allocated address.")
final class IpReleaseCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "IP allocation id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_IPS_RELEASE);
        ipamService().release(new IpAllocationId(id));
        outputWriter().write(new CliResult.Ack(id, "Address released."), out());
        return ExitCodes.OK;
    }
}
