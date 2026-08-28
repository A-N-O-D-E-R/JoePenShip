package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "iam",
        description = "Manage IAM users, roles, groups, and API credentials.",
        subcommands = {IamUserCommand.class, IamRoleCommand.class, IamGroupCommand.class, IamCredentialCommand.class})
final class IamCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
