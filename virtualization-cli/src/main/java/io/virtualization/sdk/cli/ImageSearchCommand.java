package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.image.ImageQuery;
import io.virtualization.sdk.core.image.ImageType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "search", description = "Search images.")
final class ImageSearchCommand extends AbstractCliCommand {

    @Parameters(index = "0", arity = "0..1", paramLabel = "<query>", description = "Free-text name filter.")
    String name;

    @Option(names = "--distribution", description = "Filter by distribution.")
    String distribution;

    @Option(names = "--architecture", description = "Filter by architecture.")
    String architecture;

    @Option(names = "--os", description = "Filter by operating system.")
    String os;

    @Option(names = "--version", description = "Filter by version.")
    String version;

    @Option(names = "--type", description = "Filter by image type: ${COMPLETION-CANDIDATES}.")
    ImageType type;

    @Option(names = "--remote", description = "Filter by remote.")
    String remote;

    @Override
    public Integer call() {
        ImageQuery.Builder builder = ImageQuery.builder();
        if (name != null) {
            builder.name(name);
        }
        if (distribution != null) {
            builder.distribution(distribution);
        }
        if (architecture != null) {
            builder.architecture(architecture);
        }
        if (os != null) {
            builder.operatingSystem(os);
        }
        if (version != null) {
            builder.version(version);
        }
        if (type != null) {
            builder.type(type);
        }
        if (remote != null) {
            builder.remote(remote);
        }
        outputWriter().write(new CliResult.ImageList(images().search(builder.build())), out());
        return ExitCodes.OK;
    }
}
