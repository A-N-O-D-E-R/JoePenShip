package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.RoleId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "grant-permission", description = "Grant a permission to an IAM role.")
final class IamRoleGrantPermissionCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Role id.")
    String id;

    @Option(names = "--permission", required = true, description = "Permission string, e.g. vps:create.")
    String permission;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_ROLES_GRANT_PERMISSION);
        outputWriter().write(CliResult.IamRoleView.from(roleManager().grantPermission(new RoleId(id), Permission.parse(permission))), out());
        return ExitCodes.OK;
    }
}
