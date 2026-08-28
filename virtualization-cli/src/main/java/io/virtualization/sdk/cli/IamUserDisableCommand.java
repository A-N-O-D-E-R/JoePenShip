package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.UserId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "disable", description = "Disable an IAM user.")
final class IamUserDisableCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "User id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_USERS_DISABLE);
        outputWriter().write(CliResult.IamUserView.from(userManager().setEnabled(new UserId(id), false)), out());
        return ExitCodes.OK;
    }
}
