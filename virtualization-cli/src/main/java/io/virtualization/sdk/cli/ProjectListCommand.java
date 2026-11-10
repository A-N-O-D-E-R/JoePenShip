package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List projects.")
final class ProjectListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.PROJECT_LIST);
        outputWriter().write(new CliResult.ProjectList(projectManager().list()), out());
        return ExitCodes.OK;
    }
}
