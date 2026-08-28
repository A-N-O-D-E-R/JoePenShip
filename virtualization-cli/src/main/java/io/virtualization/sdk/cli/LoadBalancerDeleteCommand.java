package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a load balancer.")
final class LoadBalancerDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "Load balancer name.")
    String name;

    @Override
    public Integer call() {
        requirePermission(Permissions.LOADBALANCER_DELETE);
        loadBalancerProvider().deleteLoadBalancer(name);
        outputWriter().write(new CliResult.Ack(name, "Load balancer deleted."), out());
        return ExitCodes.OK;
    }
}
