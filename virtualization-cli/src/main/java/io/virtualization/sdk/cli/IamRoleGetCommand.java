package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.RoleId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get an IAM role by id.")
final class IamRoleGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Role id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_ROLES_GET);
        outputWriter().write(CliResult.IamRoleView.from(roleManager().get(new RoleId(id))), out());
        return ExitCodes.OK;
    }
}
