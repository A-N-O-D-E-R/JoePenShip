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
            case CliResult.IamUserList r -> r.users();
            case CliResult.IamUserView r -> r;
            case CliResult.IamRoleList r -> r.roles();
            case CliResult.IamRoleView r -> r;
            case CliResult.IamGroupList r -> r.groups();
            case CliResult.IamGroupView r -> r;
            case CliResult.IamCredentialList r -> r.credentials();
            case CliResult.IamCredentialView r -> r;
            case CliResult.IamIssuedCredentialView r -> r;
            case CliResult.ProjectList r -> r.projects();
            case CliResult.ProjectView r -> r.project();
            case CliResult.NetworkList r -> r.networks();
            case CliResult.NetworkView r -> r.network();
            case CliResult.SubnetList r -> r.subnets();
            case CliResult.SubnetView r -> r.subnet();
            case CliResult.IpAllocationList r -> r.allocations();
            case CliResult.IpAllocationView r -> r.allocation();
            case CliResult.FirewallRuleList r -> r.rules();
            case CliResult.FirewallRuleView r -> r.rule();
            case CliResult.StoragePoolList r -> r.pools();
            case CliResult.StoragePoolView r -> r.pool();
            case CliResult.VolumeList r -> r.volumes();
            case CliResult.VolumeView r -> r.volume();
            case CliResult.VolumeSnapshotList r -> r.snapshots();
            case CliResult.VolumeSnapshotView r -> r.snapshot();
            case CliResult.LoadBalancerList r -> r.loadBalancers();
            case CliResult.LoadBalancerView r -> r.loadBalancer();
            case CliResult.RouteList r -> r.routes();
            case CliResult.RouteView r -> r.route();
        };
        try {
            out.println(mapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new OperationException("Failed to render CLI output", e);
        }
        out.flush();
    }
}
