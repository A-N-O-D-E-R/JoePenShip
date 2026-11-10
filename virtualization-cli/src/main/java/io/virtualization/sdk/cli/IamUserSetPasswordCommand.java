package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.UserId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "set-password", description = "Set an IAM user's password.")
final class IamUserSetPasswordCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "User id.")
    String id;

    @Option(names = "--password", required = true, description = "New password.")
    String password;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_USERS_SET_PASSWORD);
        outputWriter().write(CliResult.IamUserView.from(userManager().setPassword(new UserId(id), password.toCharArray())), out());
        return ExitCodes.OK;
    }
}
