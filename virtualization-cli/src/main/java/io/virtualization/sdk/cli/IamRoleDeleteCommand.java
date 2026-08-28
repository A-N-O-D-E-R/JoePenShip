package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.RoleId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete an IAM role.")
final class IamRoleDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Role id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_ROLES_DELETE);
        roleManager().delete(new RoleId(id));
        outputWriter().write(new CliResult.Ack(id, "Role deleted."), out());
        return ExitCodes.OK;
    }
}
