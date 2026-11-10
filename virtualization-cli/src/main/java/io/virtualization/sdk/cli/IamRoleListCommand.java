package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List IAM roles.")
final class IamRoleListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_ROLES_LIST);
        outputWriter().write(
                new CliResult.IamRoleList(roleManager().list().stream().map(CliResult.IamRoleView::from).toList()), out());
        return ExitCodes.OK;
    }
}
