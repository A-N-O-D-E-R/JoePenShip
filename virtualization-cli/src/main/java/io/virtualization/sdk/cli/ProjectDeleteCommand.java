package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.project.ProjectId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a project.")
final class ProjectDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Project id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.PROJECT_DELETE);
        projectManager().delete(new ProjectId(id));
        outputWriter().write(new CliResult.Ack(id, "Project deleted."), out());
        return ExitCodes.OK;
    }
}
