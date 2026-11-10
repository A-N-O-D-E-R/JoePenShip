package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "role",
        description = "Manage IAM roles.",
        subcommands = {
            IamRoleListCommand.class, IamRoleCreateCommand.class, IamRoleGetCommand.class, IamRoleDeleteCommand.class,
            IamRoleGrantPermissionCommand.class, IamRoleRevokePermissionCommand.class
        })
final class IamRoleCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
