package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.UserId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "assign-role", description = "Assign a role to an IAM user.")
final class IamUserAssignRoleCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "User id.")
    String id;

    @Parameters(index = "1", paramLabel = "<role-id>", description = "Role id.")
    String roleId;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_USERS_ASSIGN_ROLE);
        outputWriter().write(CliResult.IamUserView.from(userManager().assignRole(new UserId(id), new RoleId(roleId))), out());
        return ExitCodes.OK;
    }
}
