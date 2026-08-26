package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.image.ImageDownload;
import io.virtualization.sdk.core.image.ImageReference;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "download", description = "Download (export) an image to a local file.")
final class ImageDownloadCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<reference>", description = "Image reference, e.g. images:ubuntu/24.04.")
    String reference;

    @Option(names = "--file", required = true, description = "File to write the downloaded image to.")
    Path outputPath;

    @Override
    public Integer call() {
        String providerName = requireProviderName();
        ImageReference ref = ImageReferences.parse(providerName, reference);

        long bytes;
        String checksum;
        String checksumAlgorithm;
        try (ImageDownload download = images().download(ref)) {
            checksum = download.checksum().orElse(null);
            checksumAlgorithm = download.checksumAlgorithm().orElse(null);
            try (OutputStream fileOut = Files.newOutputStream(outputPath)) {
                bytes = download.stream().transferTo(fileOut);
            }
        } catch (IOException e) {
            throw new ConnectionException("Failed to write downloaded image to " + outputPath, e);
        }

        outputWriter().write(new CliResult.DownloadResult(outputPath.toString(), bytes, checksum, checksumAlgorithm), out());
        return ExitCodes.OK;
    }
}
