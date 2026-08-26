package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsRecordType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create a DNS record in a zone.")
final class DnsRecordCreateCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<zone>", description = "Zone name.")
    String zone;

    @Option(names = "--dns-provider", required = true, description = "Configured DNS provider name.")
    String dnsProvider;

    @Option(names = "--name", required = true, description = "Record name, relative to the zone.")
    String name;

    @Option(names = "--type", required = true, description = "Record type: ${COMPLETION-CANDIDATES}.")
    DnsRecordType type;

    @Option(names = "--value", required = true, description = "Record value.")
    String value;

    @Option(names = "--ttl", description = "TTL in seconds.")
    Long ttl;

    @Option(names = "--priority", description = "Priority (MX records only).")
    Integer priority;

    @Override
    public Integer call() {
        DnsRecord record = dnsProviderRegistry().get(dnsProvider).createRecord(zone, new DnsRecordSpec(name, type, value, ttl, priority));
        outputWriter().write(new CliResult.DnsRecordView(record), out());
        return ExitCodes.OK;
    }
}
