package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "Delete a DNS record.")
final class DnsRecordDeleteCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<zone>", description = "Zone name.")
    String zone;

    @Parameters(index = "1", paramLabel = "<id>", description = "Record id.")
    String id;

    @Option(names = "--dns-provider", required = true, description = "Configured DNS provider name.")
    String dnsProvider;

    @Override
    public Integer call() {
        requirePermission(Permissions.DNS_RECORDS_DELETE);
        dnsProviderRegistry().get(dnsProvider).deleteRecord(zone, id);
        outputWriter().write(new CliResult.Ack(id, "DNS record deleted."), out());
        return ExitCodes.OK;
    }
}
