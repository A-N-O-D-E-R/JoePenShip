package io.virtualization.sdk.cli.output;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsZone;
import io.virtualization.sdk.domain.Domain;

import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/** Renders a {@link CliResult} as a plain aligned text table. */
final class TableOutputWriter implements OutputWriter {

    @Override
    public void write(CliResult result, PrintWriter out) {
        switch (result) {
            case CliResult.VmList r -> writeVms(r.vms(), out);
            case CliResult.Vm r -> writeVms(List.of(r.vm()), out);
            case CliResult.Providers r -> writeProviders(r.providers(), out);
            case CliResult.OperationResult r -> writeOperation(r, out);
            case CliResult.ImageList r -> writeImages(r.images(), out);
            case CliResult.ImageResult r -> writeImages(List.of(r.image()), out);
            case CliResult.ImageImportResult r -> writeImageImport(r, out);
            case CliResult.WorkloadCreateResult r -> writeWorkloadCreate(r, out);
            case CliResult.DownloadResult r -> writeDownload(r, out);
            case CliResult.VpsList r -> writeVpsList(r.vpsList(), out);
            case CliResult.VpsView r -> writeVpsList(List.of(r), out);
            case CliResult.VpsCreateResult r -> writeVpsCreate(r, out);
            case CliResult.DomainList r -> writeDomains(r.domains(), out);
            case CliResult.DomainView r -> writeDomains(List.of(r.domain()), out);
            case CliResult.DnsZoneList r -> writeDnsZones(r.zones(), out);
            case CliResult.DnsRecordList r -> writeDnsRecords(r.records(), out);
            case CliResult.DnsRecordView r -> writeDnsRecords(List.of(r.record()), out);
            case CliResult.Ack r -> writeAck(r, out);
            case CliResult.CertificateList r -> writeCertificates(r.certificates(), out);
            case CliResult.CertificateView r -> writeCertificates(List.of(r.certificate()), out);
            case CliResult.CertificateExportResult r -> writeCertificateExport(r, out);
        }
        out.flush();
    }

    private void writeDomains(List<Domain> domains, PrintWriter out) {
        if (domains.isEmpty()) {
            out.println("No domains.");
            return;
        }
        out.printf("%-38s %-30s %-10s %-16s%n", "ID", "NAME", "STATUS", "DNS PROVIDER");
        for (Domain domain : domains) {
            out.printf(
                    "%-38s %-30s %-10s %-16s%n",
                    domain.id().value(), domain.name(), domain.status(), domain.dnsProvider() != null ? domain.dnsProvider() : "-");
        }
    }

    private void writeDnsZones(List<DnsZone> zones, PrintWriter out) {
        if (zones.isEmpty()) {
            out.println("No DNS zones.");
            return;
        }
        out.printf("%-30s %-16s %-20s%n", "NAME", "PROVIDER", "PROVIDER ID");
        for (DnsZone zone : zones) {
            out.printf("%-30s %-16s %-20s%n", zone.name(), zone.provider(), zone.providerId());
        }
    }

    private void writeDnsRecords(List<DnsRecord> records, PrintWriter out) {
        if (records.isEmpty()) {
            out.println("No DNS records.");
            return;
        }
        out.printf("%-10s %-20s %-24s %-6s %-30s %8s %6s%n", "ID", "ZONE", "NAME", "TYPE", "VALUE", "TTL", "PRIO");
        for (DnsRecord record : records) {
            out.printf(
                    "%-10s %-20s %-24s %-6s %-30s %8s %6s%n",
                    record.id(), record.zone(), record.name(), record.type(), truncate(record.value(), 30),
                    record.ttl() != null ? record.ttl() : "-", record.priority() != null ? record.priority() : "-");
        }
    }

    private void writeAck(CliResult.Ack ack, PrintWriter out) {
        out.printf("id:      %s%n", ack.id());
        out.printf("message: %s%n", ack.message());
    }

    private void writeCertificates(List<Certificate> certificates, PrintWriter out) {
        if (certificates.isEmpty()) {
            out.println("No certificates.");
            return;
        }
        out.printf("%-38s %-10s %-30s %-14s %-24s%n", "ID", "STATUS", "DOMAINS", "ISSUER", "EXPIRES AT");
        for (Certificate certificate : certificates) {
            out.printf(
                    "%-38s %-10s %-30s %-14s %-24s%n",
                    certificate.id().value(), certificate.status(), String.join(",", certificate.domains()),
                    certificate.issuer(), certificate.expiresAt() != null ? certificate.expiresAt() : "-");
        }
    }

    private void writeCertificateExport(CliResult.CertificateExportResult result, PrintWriter out) {
        out.printf("id:          %s%n", result.id());
        out.printf("certificate: %s%n", result.cert() ? "written" : "skipped");
        out.printf("chain:       %s%n", result.chain() ? "written" : "skipped");
        out.printf("private key: %s%n", result.privateKey() ? "written" : "skipped");
    }

    private void writeImages(List<Image> images, PrintWriter out) {
        if (images.isEmpty()) {
            out.println("No images.");
            return;
        }
        out.printf("%-16s %-24s %-16s %-10s %12s%n", "ID", "NAME", "TYPE", "ARCH", "SIZE");
        for (Image image : images) {
            out.printf(
                    "%-16s %-24s %-16s %-10s %12d%n",
                    truncate(image.id().value(), 16), image.name(), image.type(),
                    image.architecture() != null ? image.architecture() : "-", image.size());
        }
    }

    private void writeImageImport(CliResult.ImageImportResult result, PrintWriter out) {
        writeOperation(result.operation(), out);
        if (result.image() != null) {
            out.printf("image:  %s (%s)%n", result.image().name(), result.image().id().value());
        }
    }

    private void writeWorkloadCreate(CliResult.WorkloadCreateResult result, PrintWriter out) {
        writeOperation(result.operation(), out);
        if (result.workloadId() != null) {
            out.printf("workload: %s%n", result.workloadId());
        }
    }

    private void writeDownload(CliResult.DownloadResult result, PrintWriter out) {
        out.printf("path:  %s%n", result.path());
        out.printf("bytes: %d%n", result.bytes());
        if (result.checksum() != null) {
            out.printf("checksum: %s:%s%n", result.checksumAlgorithm(), result.checksum());
        }
    }

    private void writeVpsList(List<CliResult.VpsView> vpsList, PrintWriter out) {
        if (vpsList.isEmpty()) {
            out.println("No VPSs.");
            return;
        }
        out.printf("%-38s %-16s %-10s %-14s %6s %10s%n", "ID", "NAME", "STATE", "TYPE", "CPU", "MEMORY MB");
        for (CliResult.VpsView vps : vpsList) {
            out.printf(
                    "%-38s %-16s %-10s %-14s %6d %10d%n",
                    vps.id(), vps.name(), vps.state(), vps.type(), vps.cpuCores(), vps.memoryMb());
        }
    }

    private void writeVpsCreate(CliResult.VpsCreateResult result, PrintWriter out) {
        writeOperation(result.operation(), out);
        if (result.vpsId() != null) {
            out.printf("vps:    %s%n", result.vpsId());
        }
    }

    private static String truncate(String value, int length) {
        return value.length() > length ? value.substring(0, length) : value;
    }

    private void writeVms(List<VirtualMachine> vms, PrintWriter out) {
        if (vms.isEmpty()) {
            out.println("No virtual machines.");
            return;
        }
        out.printf("%-20s %-24s %-10s %6s %10s%n", "ID", "NAME", "STATE", "CPU", "MEMORY MB");
        for (VirtualMachine vm : vms) {
            out.printf(
                    "%-20s %-24s %-10s %6d %10d%n",
                    vm.id(), vm.name(), vm.state(), vm.resources().cpuCores(), vm.resources().memoryMb());
        }
    }

    private void writeProviders(List<ProviderSummary> providers, PrintWriter out) {
        if (providers.isEmpty()) {
            out.println("No providers configured.");
            return;
        }
        out.printf("%-15s %-10s %s%n", "NAME", "TYPE", "CAPABILITIES");
        for (ProviderSummary provider : providers) {
            String capabilities = provider.capabilities().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(","));
            out.printf("%-15s %-10s %s%n", provider.name(), provider.type(), capabilities);
        }
    }

    private void writeOperation(CliResult.OperationResult operation, PrintWriter out) {
        out.printf("id:     %s%n", operation.id());
        out.printf("status: %s%n", operation.status());
        if (operation.progress() != null) {
            out.printf("progress: %.0f%%%n", operation.progress() * 100);
        }
        if (operation.error() != null) {
            out.printf("error:  %s%n", operation.error());
        }
    }
}
