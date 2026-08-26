package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List images.")
final class ImageListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        outputWriter().write(new CliResult.ImageList(images().list()), out());
        return ExitCodes.OK;
    }
}
