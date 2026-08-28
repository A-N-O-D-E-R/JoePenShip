package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List IAM groups.")
final class IamGroupListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_GROUPS_LIST);
        outputWriter().write(
                new CliResult.IamGroupList(groupManager().list().stream().map(CliResult.IamGroupView::from).toList()), out());
        return ExitCodes.OK;
    }
}
