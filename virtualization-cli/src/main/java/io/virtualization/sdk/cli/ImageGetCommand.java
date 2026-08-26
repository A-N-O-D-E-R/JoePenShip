package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get an image by reference.")
final class ImageGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<reference>", description = "Image reference, e.g. images:ubuntu/24.04.")
    String reference;

    @Override
    public Integer call() {
        String providerName = requireProviderName();
        ImageReference ref = ImageReferences.parse(providerName, reference);
        Image image = client().images(providerName).get(ref)
                .orElseThrow(() -> new ResourceNotFoundException("No image '" + reference + "'"));
        outputWriter().write(new CliResult.ImageResult(image), out());
        return ExitCodes.OK;
    }
}
