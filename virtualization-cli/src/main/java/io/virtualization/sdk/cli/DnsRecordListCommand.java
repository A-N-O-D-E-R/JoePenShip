package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "list", description = "List DNS records in a zone.")
final class DnsRecordListCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<zone>", description = "Zone name.")
    String zone;

    @Option(names = "--dns-provider", required = true, description = "Configured DNS provider name.")
    String dnsProvider;

    @Override
    public Integer call() {
        outputWriter().write(new CliResult.DnsRecordList(dnsProviderRegistry().get(dnsProvider).records(zone)), out());
        return ExitCodes.OK;
    }
}
