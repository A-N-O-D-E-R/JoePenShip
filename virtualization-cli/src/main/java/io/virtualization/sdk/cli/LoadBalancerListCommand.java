package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List load balancers.")
final class LoadBalancerListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.LOADBALANCER_LIST);
        outputWriter().write(new CliResult.LoadBalancerList(loadBalancerProvider().listLoadBalancers()), out());
        return ExitCodes.OK;
    }
}
