package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.vps.CreateVpsOperation;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsManager;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;

@Command(name = "rebuild", description = "Rebuild a VPS from a (possibly different) image, destroying its current workload.")
final class VpsRebuildCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "VPS id.")
    String id;

    @Option(names = "--image", required = true, paramLabel = "<reference>", description = "Image reference, e.g. images:ubuntu/24.04.")
    String image;

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
        ImageReference ref = ImageReferences.parse(requireProviderName(), image);

        VpsManager manager = vpsManager();
        CreateVpsOperation operation = manager.rebuild(new VpsId(id), ref);
        if (wait) {
            operation.await(Duration.ofSeconds(timeoutSeconds));
            manager.get(operation.vpsId());
        }
        outputWriter().write(
                new CliResult.VpsCreateResult(CliResult.OperationResult.from(operation), operation.vpsId().value()), out());
        return operation.status() == OperationStatus.FAILED ? ExitCodes.GENERAL_ERROR : ExitCodes.OK;
    }
}
