package io.virtualization.sdk.cli.output;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsZone;
import io.virtualization.sdk.domain.Domain;
import io.virtualization.sdk.iam.ApiCredential;
import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.Group;
import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.Role;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.User;
import io.virtualization.sdk.iam.UserId;
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
import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsState;
import io.virtualization.sdk.vps.VpsType;

import java.time.Instant;
import java.util.List;

/** The shape of data a CLI command hands to an {@link OutputWriter} — one output-rendering decision per case. */
public sealed interface CliResult {

    record VmList(List<VirtualMachine> vms) implements CliResult {}

    record Vm(VirtualMachine vm) implements CliResult {}

    record Providers(List<ProviderSummary> providers) implements CliResult {}

    record OperationResult(String id, OperationStatus status, Double progress, String error) implements CliResult {

        public static OperationResult from(Operation operation) {
            return new OperationResult(
                    operation.id(),
                    operation.status(),
                    operation.progress().isPresent() ? operation.progress().getAsDouble() : null,
                    operation.error().map(Throwable::getMessage).orElse(null));
        }
    }

    record ImageList(List<Image> images) implements CliResult {}

    record ImageResult(Image image) implements CliResult {}

    record ImageImportResult(OperationResult operation, Image image) implements CliResult {}

    record WorkloadCreateResult(OperationResult operation, String workloadId) implements CliResult {}

    record DownloadResult(String path, long bytes, String checksum, String checksumAlgorithm) implements CliResult {}

    record VpsList(List<VpsView> vpsList) implements CliResult {}

    record VpsCreateResult(OperationResult operation, String vpsId) implements CliResult {}

    /**
     * Flattened view of a {@link Vps} — {@code Vps.spec()} (a hand-rolled builder class, not a
     * record) has no Jackson creator and no bean-style getters, so it can't serialize directly;
     * every field a caller would want is already flattened onto {@code Vps} itself anyway.
     */
    record VpsView(
            String id,
            String name,
            VpsState state,
            VpsType type,
            String imageProvider,
            String imageRemote,
            String imageIdentifier,
            int cpuCores,
            long memoryMb,
            long diskMb,
            String storagePool,
            String volumeType,
            String network,
            String ipv4,
            String ipv6,
            String hostname,
            String provider,
            String project,
            String workloadId,
            Instant createdAt,
            Instant updatedAt,
            Instant startedAt,
            Instant stoppedAt,
            Instant destroyedAt) implements CliResult {

        public static VpsView from(Vps vps) {
            return new VpsView(
                    vps.id().value(), vps.name(), vps.state(), vps.type(),
                    vps.image().provider(), vps.image().remote(), vps.image().identifier(),
                    vps.compute().cpuCores(), vps.compute().memoryMb(),
                    vps.storage().rootDisk().toMegabytes(), vps.storage().storagePool(), vps.storage().volumeType(),
                    vps.network().network(), vps.network().ipv4(), vps.network().ipv6(), vps.network().hostname(),
                    vps.provider(), vps.project(), vps.workloadId(),
                    vps.createdAt(), vps.updatedAt(), vps.startedAt(), vps.stoppedAt(), vps.destroyedAt());
        }
    }

    record DomainList(List<Domain> domains) implements CliResult {}

    record DomainView(Domain domain) implements CliResult {}

    record DnsZoneList(List<DnsZone> zones) implements CliResult {}

    record DnsRecordList(List<DnsRecord> records) implements CliResult {}

    record DnsRecordView(DnsRecord record) implements CliResult {}

    /** Confirms a mutation with no richer result of its own — {@code dns record delete}, {@code certificate revoke}. */
    record Ack(String id, String message) implements CliResult {}

    record CertificateList(List<Certificate> certificates) implements CliResult {}

    record CertificateView(Certificate certificate) implements CliResult {}

    record CertificateExportResult(String id, boolean cert, boolean chain, boolean privateKey) implements CliResult {}

    record IamUserList(List<IamUserView> users) implements CliResult {}

    /** {@link User} minus {@code passwordHash} — never print a password hash to stdout. */
    record IamUserView(String id, String username, List<String> roles, List<String> groups, boolean enabled, Instant createdAt)
            implements CliResult {

        public static IamUserView from(User user) {
            return new IamUserView(
                    user.id().value(), user.username(), user.roles().stream().map(RoleId::value).toList(),
                    user.groups().stream().map(GroupId::value).toList(), user.enabled(), user.createdAt());
        }
    }

    record IamRoleList(List<IamRoleView> roles) implements CliResult {}

    record IamRoleView(String id, String name, List<String> permissions, Instant createdAt) implements CliResult {

        public static IamRoleView from(Role role) {
            return new IamRoleView(role.id().value(), role.name(), role.permissions().stream().map(Permission::asString).toList(), role.createdAt());
        }
    }

    record IamGroupList(List<IamGroupView> groups) implements CliResult {}

    record IamGroupView(String id, String name, List<String> roles, List<String> members, Instant createdAt) implements CliResult {

        public static IamGroupView from(Group group) {
            return new IamGroupView(
                    group.id().value(), group.name(), group.roles().stream().map(RoleId::value).toList(),
                    group.members().stream().map(UserId::value).toList(), group.createdAt());
        }
    }

    record IamCredentialList(List<IamCredentialView> credentials) implements CliResult {}

    /** {@link ApiCredential} minus {@code tokenHash} — never print a token hash to stdout; the raw token itself is only ever shown once, from {@link IamIssuedCredentialView}. */
    record IamCredentialView(String id, String ownerId, String name, Instant createdAt, Instant expiresAt, Instant lastUsedAt) implements CliResult {

        public static IamCredentialView from(ApiCredential credential) {
            return new IamCredentialView(
                    credential.id().value(), credential.ownerId().value(), credential.name(), credential.createdAt(),
                    credential.expiresAt(), credential.lastUsedAt());
        }
    }

    record IamIssuedCredentialView(String token, IamCredentialView credential) implements CliResult {

        public static IamIssuedCredentialView from(ApiCredentialManager.IssuedApiCredential issued) {
            return new IamIssuedCredentialView(issued.rawToken(), IamCredentialView.from(issued.credential()));
        }
    }

    record ProjectList(List<Project> projects) implements CliResult {}

    record ProjectView(Project project) implements CliResult {}

    record NetworkList(List<Network> networks) implements CliResult {}

    record NetworkView(Network network) implements CliResult {}

    record SubnetList(List<Subnet> subnets) implements CliResult {}

    record SubnetView(Subnet subnet) implements CliResult {}

    record IpAllocationList(List<IpAllocation> allocations) implements CliResult {}

    record IpAllocationView(IpAllocation allocation) implements CliResult {}

    record FirewallRuleList(List<FirewallRule> rules) implements CliResult {}

    record FirewallRuleView(FirewallRule rule) implements CliResult {}

    record StoragePoolList(List<StoragePool> pools) implements CliResult {}

    record StoragePoolView(StoragePool pool) implements CliResult {}

    record VolumeList(List<Volume> volumes) implements CliResult {}

    record VolumeView(Volume volume) implements CliResult {}

    record VolumeSnapshotList(List<VolumeSnapshot> snapshots) implements CliResult {}

    record VolumeSnapshotView(VolumeSnapshot snapshot) implements CliResult {}

    record LoadBalancerList(List<LoadBalancer> loadBalancers) implements CliResult {}

    record LoadBalancerView(LoadBalancer loadBalancer) implements CliResult {}

    record RouteList(List<Route> routes) implements CliResult {}

    record RouteView(Route route) implements CliResult {}
}
