package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create an IAM group.")
final class IamGroupCreateCommand extends AbstractCliCommand {

    @Option(names = "--name", required = true, description = "Group name.")
    String name;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_GROUPS_CREATE);
        outputWriter().write(CliResult.IamGroupView.from(groupManager().create(name)), out());
        return ExitCodes.OK;
    }
}
