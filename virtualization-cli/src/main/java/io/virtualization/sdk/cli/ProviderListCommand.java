package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.cli.output.ProviderSummary;
import picocli.CommandLine.Command;

import java.util.Comparator;
import java.util.List;

@Command(name = "list", description = "List configured providers.")
final class ProviderListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        List<ProviderSummary> summaries = client().providers().entrySet().stream()
                .map(e -> new ProviderSummary(e.getKey(), e.getValue().type().id(), e.getValue().capabilities().all()))
                .sorted(Comparator.comparing(ProviderSummary::name))
                .toList();
        outputWriter().write(new CliResult.Providers(summaries), out());
        return ExitCodes.OK;
    }
}
