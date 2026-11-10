package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "group",
        description = "Manage IAM groups.",
        subcommands = {
            IamGroupListCommand.class, IamGroupCreateCommand.class, IamGroupGetCommand.class, IamGroupDeleteCommand.class,
            IamGroupAddMemberCommand.class, IamGroupRemoveMemberCommand.class, IamGroupAssignRoleCommand.class,
            IamGroupRemoveRoleCommand.class
        })
final class IamGroupCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
