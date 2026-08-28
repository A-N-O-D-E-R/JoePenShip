package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a route.")
final class RouteDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<host>", description = "Route host.")
    String host;

    @Option(names = "--load-balancer", required = true, description = "Load balancer name.")
    String loadBalancer;

    @Override
    public Integer call() {
        requirePermission(Permissions.LOADBALANCER_ROUTES_DELETE);
        loadBalancerProvider().deleteRoute(loadBalancer, host);
        outputWriter().write(new CliResult.Ack(host, "Route deleted."), out());
        return ExitCodes.OK;
    }
}
