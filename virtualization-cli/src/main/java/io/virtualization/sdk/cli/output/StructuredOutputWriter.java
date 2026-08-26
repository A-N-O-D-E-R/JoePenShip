package io.virtualization.sdk.cli.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.core.exception.OperationException;

import java.io.PrintWriter;

/** Renders a {@link CliResult} as JSON or YAML — same logic either way, only the {@link ObjectMapper} differs. */
final class StructuredOutputWriter implements OutputWriter {

    private final ObjectMapper mapper;

    StructuredOutputWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void write(CliResult result, PrintWriter out) {
        Object payload = switch (result) {
            case CliResult.VmList r -> r.vms();
            case CliResult.Vm r -> r.vm();
            case CliResult.Providers r -> r.providers();
            case CliResult.OperationResult r -> r;
            case CliResult.ImageList r -> r.images();
            case CliResult.ImageResult r -> r.image();
            case CliResult.ImageImportResult r -> r;
            case CliResult.WorkloadCreateResult r -> r;
            case CliResult.DownloadResult r -> r;
            case CliResult.VpsList r -> r.vpsList();
            case CliResult.VpsView r -> r;
            case CliResult.VpsCreateResult r -> r;
            case CliResult.DomainList r -> r.domains();
            case CliResult.DomainView r -> r.domain();
            case CliResult.DnsZoneList r -> r.zones();
            case CliResult.DnsRecordList r -> r.records();
            case CliResult.DnsRecordView r -> r.record();
            case CliResult.Ack r -> r;
            case CliResult.CertificateList r -> r.certificates();
            case CliResult.CertificateView r -> r.certificate();
            case CliResult.CertificateExportResult r -> r;
        };
        try {
            out.println(mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new OperationException("Failed to render CLI output", e);
        }
        out.flush();
    }
}
