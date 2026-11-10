package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a route by host.")
final class RouteGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<host>", description = "Route host.")
    String host;

    @Option(names = "--load-balancer", required = true, description = "Load balancer name.")
    String loadBalancer;

    @Override
    public Integer call() {
        requirePermission(Permissions.LOADBALANCER_ROUTES_GET);
        outputWriter().write(new CliResult.RouteView(loadBalancerProvider().getRoute(loadBalancer, host)), out());
        return ExitCodes.OK;
    }
}
