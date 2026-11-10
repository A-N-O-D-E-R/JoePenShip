package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.User;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create an IAM user.")
final class IamUserCreateCommand extends AbstractCliCommand {

    @Option(names = "--username", required = true, description = "Username.")
    String username;

    @Option(names = "--password", required = true, description = "Initial password.")
    String password;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_USERS_CREATE);
        User user = userManager().create(username, password.toCharArray());
        outputWriter().write(CliResult.IamUserView.from(user), out());
        return ExitCodes.OK;
    }
}
