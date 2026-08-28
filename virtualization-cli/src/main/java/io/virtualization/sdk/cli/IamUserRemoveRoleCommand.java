package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.UserId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "remove-role", description = "Remove a role from an IAM user.")
final class IamUserRemoveRoleCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "User id.")
    String id;

    @Parameters(index = "1", paramLabel = "<role-id>", description = "Role id.")
    String roleId;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_USERS_REMOVE_ROLE);
        outputWriter().write(CliResult.IamUserView.from(userManager().removeRole(new UserId(id), new RoleId(roleId))), out());
        return ExitCodes.OK;
    }
}
