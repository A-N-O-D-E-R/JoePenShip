package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.UserId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "add-member", description = "Add a user to an IAM group.")
final class IamGroupAddMemberCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Group id.")
    String id;

    @Parameters(index = "1", paramLabel = "<user-id>", description = "User id.")
    String userId;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_GROUPS_ADD_MEMBER);
        outputWriter().write(CliResult.IamGroupView.from(groupManager().addMember(new GroupId(id), new UserId(userId))), out());
        return ExitCodes.OK;
    }
}
