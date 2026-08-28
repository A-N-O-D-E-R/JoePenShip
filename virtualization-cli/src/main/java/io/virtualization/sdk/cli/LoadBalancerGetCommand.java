package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a load balancer by name.")
final class LoadBalancerGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "Load balancer name.")
    String name;

    @Override
    public Integer call() {
        requirePermission(Permissions.LOADBALANCER_GET);
        outputWriter().write(new CliResult.LoadBalancerView(loadBalancerProvider().getLoadBalancer(name)), out());
        return ExitCodes.OK;
    }
}
