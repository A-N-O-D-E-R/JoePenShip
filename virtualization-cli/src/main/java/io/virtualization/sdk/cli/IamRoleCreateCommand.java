package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create an IAM role.")
final class IamRoleCreateCommand extends AbstractCliCommand {

    @Option(names = "--name", required = true, description = "Role name.")
    String name;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_ROLES_CREATE);
        outputWriter().write(CliResult.IamRoleView.from(roleManager().create(name)), out());
        return ExitCodes.OK;
    }
}
