package io.virtualization.sdk.cli.output;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsZone;
import io.virtualization.sdk.domain.Domain;
import io.virtualization.sdk.loadbalancer.LoadBalancer;
import io.virtualization.sdk.loadbalancer.Route;
import io.virtualization.sdk.network.FirewallRule;
import io.virtualization.sdk.network.IpAllocation;
import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.Subnet;
import io.virtualization.sdk.project.Project;
import io.virtualization.sdk.storage.StoragePool;
import io.virtualization.sdk.storage.Volume;
import io.virtualization.sdk.storage.VolumeSnapshot;

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
            case CliResult.IamUserList r -> writeIamUsers(r.users(), out);
            case CliResult.IamUserView r -> writeIamUsers(List.of(r), out);
            case CliResult.IamRoleList r -> writeIamRoles(r.roles(), out);
            case CliResult.IamRoleView r -> writeIamRoles(List.of(r), out);
            case CliResult.IamGroupList r -> writeIamGroups(r.groups(), out);
            case CliResult.IamGroupView r -> writeIamGroups(List.of(r), out);
            case CliResult.IamCredentialList r -> writeIamCredentials(r.credentials(), out);
            case CliResult.IamCredentialView r -> writeIamCredentials(List.of(r), out);
            case CliResult.IamIssuedCredentialView r -> writeIamIssuedCredential(r, out);
            case CliResult.ProjectList r -> writeProjects(r.projects(), out);
            case CliResult.ProjectView r -> writeProjects(List.of(r.project()), out);
            case CliResult.NetworkList r -> writeNetworks(r.networks(), out);
            case CliResult.NetworkView r -> writeNetworks(List.of(r.network()), out);
            case CliResult.SubnetList r -> writeSubnets(r.subnets(), out);
            case CliResult.SubnetView r -> writeSubnets(List.of(r.subnet()), out);
            case CliResult.IpAllocationList r -> writeIpAllocations(r.allocations(), out);
            case CliResult.IpAllocationView r -> writeIpAllocations(List.of(r.allocation()), out);
            case CliResult.FirewallRuleList r -> writeFirewallRules(r.rules(), out);
            case CliResult.FirewallRuleView r -> writeFirewallRules(List.of(r.rule()), out);
            case CliResult.StoragePoolList r -> writeStoragePools(r.pools(), out);
            case CliResult.StoragePoolView r -> writeStoragePools(List.of(r.pool()), out);
            case CliResult.VolumeList r -> writeVolumes(r.volumes(), out);
            case CliResult.VolumeView r -> writeVolumes(List.of(r.volume()), out);
            case CliResult.VolumeSnapshotList r -> writeVolumeSnapshots(r.snapshots(), out);
            case CliResult.VolumeSnapshotView r -> writeVolumeSnapshots(List.of(r.snapshot()), out);
            case CliResult.LoadBalancerList r -> writeLoadBalancers(r.loadBalancers(), out);
            case CliResult.LoadBalancerView r -> writeLoadBalancers(List.of(r.loadBalancer()), out);
            case CliResult.RouteList r -> writeRoutes(r.routes(), out);
            case CliResult.RouteView r -> writeRoutes(List.of(r.route()), out);
        }
        out.flush();
    }

    private void writeLoadBalancers(List<LoadBalancer> loadBalancers, PrintWriter out) {
        if (loadBalancers.isEmpty()) {
            out.println("No load balancers.");
            return;
        }
        out.printf("%-20s %s%n", "NAME", "LISTEN");
        for (LoadBalancer loadBalancer : loadBalancers) {
            out.printf("%-20s %s%n", loadBalancer.name(), String.join(",", loadBalancer.listenAddresses()));
        }
    }

    private void writeRoutes(List<Route> routes, PrintWriter out) {
        if (routes.isEmpty()) {
            out.println("No routes.");
            return;
        }
        out.printf("%-30s %-40s %s%n", "HOST", "BACKENDS", "HEALTH CHECK");
        for (Route route : routes) {
            String backends = route.backends().stream().map(b -> b.host() + ":" + b.port()).collect(Collectors.joining(","));
            String healthCheck = route.healthCheck() != null ? route.healthCheck().path() : "-";
            out.printf("%-30s %-40s %s%n", route.host(), backends, healthCheck);
        }
    }

    private void writeStoragePools(List<StoragePool> pools, PrintWriter out) {
        if (pools.isEmpty()) {
            out.println("No storage pools.");
            return;
        }
        out.printf("%-20s %-10s %-10s %s%n", "NAME", "DRIVER", "STATUS", "DESCRIPTION");
        for (StoragePool pool : pools) {
            out.printf("%-20s %-10s %-10s %s%n", pool.name(), pool.driver(), pool.status(), pool.description() != null ? pool.description() : "-");
        }
    }

    private void writeVolumes(List<Volume> volumes, PrintWriter out) {
        if (volumes.isEmpty()) {
            out.println("No volumes.");
            return;
        }
        out.printf("%-20s %-14s %-10s %-12s %s%n", "NAME", "POOL", "TYPE", "CONTENT TYPE", "SIZE");
        for (Volume volume : volumes) {
            out.printf(
                    "%-20s %-14s %-10s %-12s %s%n", volume.name(), volume.pool(), volume.type(), volume.contentType(),
                    volume.size() != null ? volume.size() : "-");
        }
    }

    private void writeVolumeSnapshots(List<VolumeSnapshot> snapshots, PrintWriter out) {
        if (snapshots.isEmpty()) {
            out.println("No snapshots.");
            return;
        }
        out.printf("%-20s %-20s %-14s %s%n", "NAME", "VOLUME", "POOL", "CREATED AT");
        for (VolumeSnapshot snapshot : snapshots) {
            out.printf(
                    "%-20s %-20s %-14s %s%n", snapshot.name(), snapshot.volumeName(), snapshot.pool(),
                    snapshot.createdAt() != null ? snapshot.createdAt() : "-");
        }
    }

    private void writeNetworks(List<Network> networks, PrintWriter out) {
        if (networks.isEmpty()) {
            out.println("No networks.");
            return;
        }
        out.printf("%-38s %-20s %s%n", "ID", "NAME", "DESCRIPTION");
        for (Network network : networks) {
            out.printf("%-38s %-20s %s%n", network.id().value(), network.name(), network.description() != null ? network.description() : "-");
        }
    }

    private void writeSubnets(List<Subnet> subnets, PrintWriter out) {
        if (subnets.isEmpty()) {
            out.println("No subnets.");
            return;
        }
        out.printf("%-38s %-38s %-20s %s%n", "ID", "NETWORK ID", "CIDR", "GATEWAY");
        for (Subnet subnet : subnets) {
            out.printf(
                    "%-38s %-38s %-20s %s%n", subnet.id().value(), subnet.networkId().value(), subnet.cidr(),
                    subnet.gateway() != null ? subnet.gateway() : "-");
        }
    }

    private void writeIpAllocations(List<IpAllocation> allocations, PrintWriter out) {
        if (allocations.isEmpty()) {
            out.println("No IP allocations.");
            return;
        }
        out.printf("%-38s %-38s %-16s %-24s %s%n", "ID", "SUBNET ID", "ADDRESS", "OWNER", "ALLOCATED AT");
        for (IpAllocation allocation : allocations) {
            out.printf(
                    "%-38s %-38s %-16s %-24s %s%n", allocation.id().value(), allocation.subnetId().value(),
                    allocation.address(), allocation.ownerRef(), allocation.allocatedAt());
        }
    }

    private void writeFirewallRules(List<FirewallRule> rules, PrintWriter out) {
        if (rules.isEmpty()) {
            out.println("No firewall rules.");
            return;
        }
        out.printf("%-38s %-38s %-9s %-8s %-11s %-20s %-6s %s%n", "ID", "NETWORK ID", "DIRECTION", "PROTOCOL", "PORT RANGE", "CIDR", "ACTION", "PRIORITY");
        for (FirewallRule rule : rules) {
            String portRange = rule.portRangeStart() != null ? rule.portRangeStart() + "-" + rule.portRangeEnd() : "-";
            out.printf(
                    "%-38s %-38s %-9s %-8s %-11s %-20s %-6s %d%n", rule.id().value(), rule.networkId().value(), rule.direction(),
                    rule.protocol(), portRange, rule.cidr(), rule.action(), rule.priority());
        }
    }

    private void writeProjects(List<Project> projects, PrintWriter out) {
        if (projects.isEmpty()) {
            out.println("No projects.");
            return;
        }
        out.printf("%-38s %-20s %s%n", "ID", "NAME", "DESCRIPTION");
        for (Project project : projects) {
            out.printf("%-38s %-20s %s%n", project.id().value(), project.name(), project.description() != null ? project.description() : "-");
        }
    }

    private void writeIamUsers(List<CliResult.IamUserView> users, PrintWriter out) {
        if (users.isEmpty()) {
            out.println("No users.");
            return;
        }
        out.printf("%-38s %-16s %-8s %s%n", "ID", "USERNAME", "ENABLED", "ROLES");
        for (CliResult.IamUserView user : users) {
            out.printf("%-38s %-16s %-8s %s%n", user.id(), user.username(), user.enabled(), String.join(",", user.roles()));
        }
    }

    private void writeIamRoles(List<CliResult.IamRoleView> roles, PrintWriter out) {
        if (roles.isEmpty()) {
            out.println("No roles.");
            return;
        }
        out.printf("%-38s %-20s %s%n", "ID", "NAME", "PERMISSIONS");
        for (CliResult.IamRoleView role : roles) {
            out.printf("%-38s %-20s %s%n", role.id(), role.name(), String.join(",", role.permissions()));
        }
    }

    private void writeIamGroups(List<CliResult.IamGroupView> groups, PrintWriter out) {
        if (groups.isEmpty()) {
            out.println("No groups.");
            return;
        }
        out.printf("%-38s %-20s %-30s %s%n", "ID", "NAME", "ROLES", "MEMBERS");
        for (CliResult.IamGroupView group : groups) {
            out.printf(
                    "%-38s %-20s %-30s %s%n", group.id(), group.name(), String.join(",", group.roles()),
                    String.join(",", group.members()));
        }
    }

    private void writeIamCredentials(List<CliResult.IamCredentialView> credentials, PrintWriter out) {
        if (credentials.isEmpty()) {
            out.println("No API credentials.");
            return;
        }
        out.printf("%-38s %-38s %-20s %-24s %s%n", "ID", "OWNER ID", "NAME", "EXPIRES AT", "LAST USED AT");
        for (CliResult.IamCredentialView credential : credentials) {
            out.printf(
                    "%-38s %-38s %-20s %-24s %s%n", credential.id(), credential.ownerId(), credential.name(),
                    credential.expiresAt() != null ? credential.expiresAt() : "-",
                    credential.lastUsedAt() != null ? credential.lastUsedAt() : "-");
        }
    }

    private void writeIamIssuedCredential(CliResult.IamIssuedCredentialView result, PrintWriter out) {
        out.printf("id:    %s%n", result.credential().id());
        out.printf("name:  %s%n", result.credential().name());
        out.printf("token: %s%n", result.token());
        out.println("Save this now — it will not be shown again.");
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
