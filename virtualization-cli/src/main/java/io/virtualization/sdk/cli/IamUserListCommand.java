package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List IAM users.")
final class IamUserListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_USERS_LIST);
        outputWriter().write(
                new CliResult.IamUserList(userManager().list().stream().map(CliResult.IamUserView::from).toList()), out());
        return ExitCodes.OK;
    }
}
