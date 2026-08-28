package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.RoleId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "remove-role", description = "Remove a role from an IAM group.")
final class IamGroupRemoveRoleCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Group id.")
    String id;

    @Parameters(index = "1", paramLabel = "<role-id>", description = "Role id.")
    String roleId;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_GROUPS_REMOVE_ROLE);
        outputWriter().write(CliResult.IamGroupView.from(groupManager().removeRole(new GroupId(id), new RoleId(roleId))), out());
        return ExitCodes.OK;
    }
}
