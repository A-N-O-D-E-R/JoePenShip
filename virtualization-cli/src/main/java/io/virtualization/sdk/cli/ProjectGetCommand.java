package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.project.ProjectId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a project by id.")
final class ProjectGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Project id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.PROJECT_GET);
        outputWriter().write(new CliResult.ProjectView(projectManager().get(new ProjectId(id))), out());
        return ExitCodes.OK;
    }
}
