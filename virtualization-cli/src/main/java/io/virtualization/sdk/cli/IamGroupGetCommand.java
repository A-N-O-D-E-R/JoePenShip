package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get an IAM group by id.")
final class IamGroupGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Group id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_GROUPS_GET);
        outputWriter().write(CliResult.IamGroupView.from(groupManager().get(new GroupId(id))), out());
        return ExitCodes.OK;
    }
}
