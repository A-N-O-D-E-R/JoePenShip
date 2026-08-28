package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.UserId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List an IAM user's API credentials.")
final class IamCredentialListCommand extends AbstractCliCommand {

    @Option(names = "--owner-id", required = true, description = "Owning user id.")
    String ownerId;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_CREDENTIALS_LIST);
        outputWriter().write(
                new CliResult.IamCredentialList(
                        apiCredentialManager().list(new UserId(ownerId)).stream().map(CliResult.IamCredentialView::from).toList()),
                out());
        return ExitCodes.OK;
    }
}
