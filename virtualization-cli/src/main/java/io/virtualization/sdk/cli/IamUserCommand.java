package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "user",
        description = "Manage IAM users.",
        subcommands = {
            IamUserListCommand.class, IamUserCreateCommand.class, IamUserGetCommand.class, IamUserEnableCommand.class,
            IamUserDisableCommand.class, IamUserSetPasswordCommand.class, IamUserAssignRoleCommand.class, IamUserRemoveRoleCommand.class
        })
final class IamUserCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
