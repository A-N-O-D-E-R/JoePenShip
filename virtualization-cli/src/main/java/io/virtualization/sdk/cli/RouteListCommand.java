package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List routes on a load balancer.")
final class RouteListCommand extends AbstractCliCommand {

    @Option(names = "--load-balancer", required = true, description = "Load balancer name.")
    String loadBalancer;

    @Override
    public Integer call() {
        requirePermission(Permissions.LOADBALANCER_ROUTES_LIST);
        outputWriter().write(new CliResult.RouteList(loadBalancerProvider().listRoutes(loadBalancer)), out());
        return ExitCodes.OK;
    }
}
