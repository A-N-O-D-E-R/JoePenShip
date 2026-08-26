package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "image",
        description = "Manage images.",
        subcommands = {
            ImageListCommand.class,
            ImageSearchCommand.class,
            ImageGetCommand.class,
            ImagePullCommand.class,
            ImageDownloadCommand.class,
            ImageImportCommand.class
        })
final class ImageCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
