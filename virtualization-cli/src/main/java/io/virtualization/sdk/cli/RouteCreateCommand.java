package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.loadbalancer.Backend;
import io.virtualization.sdk.loadbalancer.HealthCheck;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;
import java.util.List;

@Command(name = "create", description = "Create a route on a load balancer.")
final class RouteCreateCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<host>", description = "Route host, e.g. app.example.com.")
    String host;

    @Option(names = "--load-balancer", required = true, description = "Load balancer name.")
    String loadBalancer;

    @Option(names = "--backend", required = true, description = "Backend as <host:port>. Repeatable.")
    List<String> backends;

    @Option(names = "--health-check-path", description = "Active health check path, e.g. /healthz.")
    String healthCheckPath;

    @Option(names = "--health-check-interval", description = "Active health check interval in seconds.")
    Long healthCheckIntervalSeconds;

    @Option(names = "--health-check-timeout", description = "Active health check timeout in seconds.")
    Long healthCheckTimeoutSeconds;

    @Override
    public Integer call() {
        requirePermission(Permissions.LOADBALANCER_ROUTES_CREATE);
        List<Backend> parsedBackends = backends.stream().map(RouteCreateCommand::parseBackend).toList();
        outputWriter().write(new CliResult.RouteView(
                loadBalancerProvider().createRoute(loadBalancer, host, parsedBackends, healthCheck())), out());
        return ExitCodes.OK;
    }

    private HealthCheck healthCheck() {
        if (healthCheckPath == null && healthCheckIntervalSeconds == null && healthCheckTimeoutSeconds == null) {
            return null;
        }
        if (healthCheckPath == null || healthCheckIntervalSeconds == null || healthCheckTimeoutSeconds == null) {
            throw new IllegalArgumentException(
                    "--health-check-path, --health-check-interval and --health-check-timeout must be given together");
        }
        return new HealthCheck(healthCheckPath, Duration.ofSeconds(healthCheckIntervalSeconds), Duration.ofSeconds(healthCheckTimeoutSeconds));
    }

    static Backend parseBackend(String value) {
        int colon = value.lastIndexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("--backend must be '<host:port>', got '" + value + "'");
        }
        return new Backend(value.substring(0, colon), Integer.parseInt(value.substring(colon + 1)));
    }
}
