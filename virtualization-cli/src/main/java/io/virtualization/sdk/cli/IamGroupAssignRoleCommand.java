package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.RoleId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "assign-role", description = "Assign a role to an IAM group.")
final class IamGroupAssignRoleCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Group id.")
    String id;

    @Parameters(index = "1", paramLabel = "<role-id>", description = "Role id.")
    String roleId;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_GROUPS_ASSIGN_ROLE);
        outputWriter().write(CliResult.IamGroupView.from(groupManager().assignRole(new GroupId(id), new RoleId(roleId))), out());
        return ExitCodes.OK;
    }
}
