package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "create", description = "Create a project.")
final class ProjectCreateCommand extends AbstractCliCommand {

    @Option(names = "--name", required = true, description = "Project name.")
    String name;

    @Option(names = "--description", description = "Free-text description.")
    String description;

    @Override
    public Integer call() {
        requirePermission(Permissions.PROJECT_CREATE);
        outputWriter().write(new CliResult.ProjectView(projectManager().create(name, description)), out());
        return ExitCodes.OK;
    }
}
