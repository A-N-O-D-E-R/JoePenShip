package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "credential",
        description = "Manage IAM API credentials.",
        subcommands = {IamCredentialIssueCommand.class, IamCredentialListCommand.class, IamCredentialRevokeCommand.class})
final class IamCredentialCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
