package io.virtualization.sdk.cli.output;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsZone;
import io.virtualization.sdk.domain.Domain;
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
}
