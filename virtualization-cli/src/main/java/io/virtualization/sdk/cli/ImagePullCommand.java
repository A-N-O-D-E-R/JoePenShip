package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.image.ImagePullOperation;
import io.virtualization.sdk.core.image.ImageReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;

@Command(name = "pull", description = "Pull an image from a remote into the provider's local store.")
final class ImagePullCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<reference>", description = "Image reference, e.g. images:ubuntu/24.04.")
    String reference;

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
        String providerName = requireProviderName();
        ImageReference ref = ImageReferences.parse(providerName, reference);

        ImagePullOperation operation = images().pull(ref);
        if (wait) {
            operation.await(Duration.ofSeconds(timeoutSeconds));
        }
        outputWriter().write(CliResult.OperationResult.from(operation), out());
        return operation.status() == OperationStatus.FAILED ? ExitCodes.GENERAL_ERROR : ExitCodes.OK;
    }
}
