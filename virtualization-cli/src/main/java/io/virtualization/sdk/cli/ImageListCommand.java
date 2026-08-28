package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List images.")
final class ImageListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        requirePermission(Permissions.IMAGE_LIST);
        outputWriter().write(new CliResult.ImageList(images().list()), out());
        return ExitCodes.OK;
    }
}
