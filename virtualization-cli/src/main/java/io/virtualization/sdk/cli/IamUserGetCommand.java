package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.UserId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get an IAM user by id.")
final class IamUserGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "User id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_USERS_GET);
        outputWriter().write(CliResult.IamUserView.from(userManager().get(new UserId(id))), out());
        return ExitCodes.OK;
    }
}
