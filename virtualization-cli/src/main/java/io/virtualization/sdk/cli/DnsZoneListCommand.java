package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List DNS zones for a provider.")
final class DnsZoneListCommand extends AbstractCliCommand {

    @Option(names = "--dns-provider", required = true, description = "Configured DNS provider name.")
    String dnsProvider;

    @Override
    public Integer call() {
        outputWriter().write(new CliResult.DnsZoneList(dnsProviderRegistry().get(dnsProvider).zones()), out());
        return ExitCodes.OK;
    }
}
