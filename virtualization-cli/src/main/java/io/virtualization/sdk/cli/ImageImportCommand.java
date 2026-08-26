package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageImportOperation;
import io.virtualization.sdk.core.image.LocalFileImageSource;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.nio.file.Path;
import java.time.Duration;

@Command(name = "import", description = "Import a local image file.")
final class ImageImportCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<file>", description = "Local image file to import.")
    Path file;

    @Option(
            names = "--wait",
            negatable = true,
            defaultValue = "true",
            description = "Wait for the operation to complete (default), or return immediately with --no-wait.")
    boolean wait;

    @Option(names = "--timeout", defaultValue = "600", description = "Seconds to wait for completion (default: 600).")
    long timeoutSeconds;

    @Override
    public Integer call() {
        requireProviderName();
        ImageImportOperation operation = images().importImage(new LocalFileImageSource(file));
        if (wait) {
            operation.await(Duration.ofSeconds(timeoutSeconds));
        }
        Image image = operation.result().orElse(null);
        outputWriter().write(new CliResult.ImageImportResult(CliResult.OperationResult.from(operation), image), out());
        return operation.status() == OperationStatus.FAILED ? ExitCodes.GENERAL_ERROR : ExitCodes.OK;
    }
}
