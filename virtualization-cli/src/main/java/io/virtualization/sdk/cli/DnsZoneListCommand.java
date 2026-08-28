package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "List DNS zones for a provider.")
final class DnsZoneListCommand extends AbstractCliCommand {

    @Option(names = "--dns-provider", required = true, description = "Configured DNS provider name.")
    String dnsProvider;

    @Override
    public Integer call() {
        requirePermission(Permissions.DNS_ZONES_LIST);
        outputWriter().write(new CliResult.DnsZoneList(dnsProviderRegistry().get(dnsProvider).zones()), out());
        return ExitCodes.OK;
    }
}
