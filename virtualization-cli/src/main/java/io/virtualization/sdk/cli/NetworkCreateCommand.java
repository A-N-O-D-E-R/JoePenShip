package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create a network.")
final class NetworkCreateCommand extends AbstractCliCommand {

    @Option(names = "--name", required = true, description = "Network name.")
    String name;

    @Option(names = "--description", description = "Free-text description.")
    String description;

    @Override
    public Integer call() {
        requirePermission(Permissions.NETWORK_CREATE);
        outputWriter().write(new CliResult.NetworkView(networkManager().create(name, description)), out());
        return ExitCodes.OK;
    }
}
