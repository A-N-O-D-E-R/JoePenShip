package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@Command(name = "create", description = "Create a load balancer.")
final class LoadBalancerCreateCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<name>", description = "Load balancer name.")
    String name;

    @Option(names = "--listen", required = true, description = "Listen address, e.g. :443. Repeatable.")
    List<String> listenAddresses;

    @Override
    public Integer call() {
        requirePermission(Permissions.LOADBALANCER_CREATE);
        outputWriter().write(new CliResult.LoadBalancerView(loadBalancerProvider().createLoadBalancer(name, listenAddresses)), out());
        return ExitCodes.OK;
    }
}
