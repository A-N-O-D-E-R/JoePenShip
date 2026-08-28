package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete an IAM group.")
final class IamGroupDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Group id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_GROUPS_DELETE);
        groupManager().delete(new GroupId(id));
        outputWriter().write(new CliResult.Ack(id, "Group deleted."), out());
        return ExitCodes.OK;
    }
}
