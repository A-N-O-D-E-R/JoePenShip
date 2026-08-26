package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.vps.CreateVpsOperation;
import io.virtualization.sdk.vps.DataSize;
import io.virtualization.sdk.vps.NetworkConfiguration;
import io.virtualization.sdk.vps.VpsManager;
import io.virtualization.sdk.vps.VpsSpec;
import io.virtualization.sdk.vps.VpsType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.util.List;

@Command(name = "create", description = "Create a VPS from an image.")
final class VpsCreateCommand extends AbstractCliCommand {

    @Option(names = "--image", required = true, paramLabel = "<reference>", description = "Image reference, e.g. images:ubuntu/24.04.")
    String image;

    @Option(names = "--name", required = true, description = "Name for the new VPS.")
    String name;

    @Option(names = "--type", defaultValue = "VIRTUAL_MACHINE", description = "VPS type: ${COMPLETION-CANDIDATES} (default: VIRTUAL_MACHINE).")
    VpsType type;

    @Option(names = "--cpu", description = "Number of CPU cores.")
    Integer cpu;

    @Option(names = "--memory-mb", description = "Memory in megabytes.")
    Long memoryMb;

    @Option(names = "--disk-mb", description = "Root disk size in megabytes.")
    Long diskMb;

    @Option(names = "--storage-pool", description = "Provider storage pool.")
    String storagePool;

    @Option(names = "--volume-type", description = "Provider volume type.")
    String volumeType;

    @Option(names = "--network", description = "Network to attach.")
    String network;

    @Option(names = "--ipv4", description = "Static IPv4 address.")
    String ipv4;

    @Option(names = "--ipv6", description = "Static IPv6 address.")
    String ipv6;

    @Option(names = "--hostname", description = "Guest hostname (also used for cloud-init).")
    String hostname;

    @Option(names = "--ssh-key", description = "SSH public key to inject, repeatable.")
    List<String> sshPublicKeys = List.of();

    @Option(names = "--cloud-init", description = "Raw #cloud-config document; overrides --ssh-key/--hostname synthesis.")
    String cloudInit;

    @Option(names = "--location", description = "Provider location/region.")
    String location;

    @Option(names = "--project", description = "Provider project.")
    String project;

    @Option(names = "--idempotency-key", description = "Repeat the same key to make create() safe to retry.")
    String idempotencyKey;

    @Option(
            names = "--wait",
            negatable = true,
            defaultValue = "true",
            description = "Wait for the operation to complete (default), or return immediately with --no-wait.")
    boolean wait;

    @Option(names = "--timeout", defaultValue = "600", description = "Seconds to wait for completion (default: 600).")
    long timeoutSeconds;

    @Override
    public Integer call() {
        String providerName = requireProviderName();
        ImageReference ref = ImageReferences.parse(providerName, image);

        VpsSpec.Builder builder = VpsSpec.builder(name, ref).type(type);
        if (cpu != null) {
            builder.cpu(cpu);
        }
        if (memoryMb != null) {
            builder.memory(DataSize.ofMegabytes(memoryMb));
        }
        if (diskMb != null) {
            builder.disk(DataSize.ofMegabytes(diskMb));
        }
        if (storagePool != null) {
            builder.storagePool(storagePool);
        }
        if (volumeType != null) {
            builder.volumeType(volumeType);
        }
        if (network != null || ipv4 != null || ipv6 != null || hostname != null) {
            builder.network(new NetworkConfiguration(network, ipv4, ipv6, hostname));
        }
        if (!sshPublicKeys.isEmpty()) {
            builder.sshPublicKeys(sshPublicKeys);
        }
        if (cloudInit != null) {
            builder.cloudInit(cloudInit);
        }
        if (location != null) {
            builder.location(location);
        }
        if (project != null) {
            builder.project(project);
        }
        if (idempotencyKey != null) {
            builder.idempotencyKey(idempotencyKey);
        }

        VpsManager manager = vpsManager();
        CreateVpsOperation operation = manager.create(builder.build());
        if (wait) {
            operation.await(Duration.ofSeconds(timeoutSeconds));
            // the CLI process exits right after this — force the reconciled terminal state into
            // the JSON file now, since there's no later invocation of *this* VpsManager to do it.
            manager.get(operation.vpsId());
        }
        outputWriter().write(
                new CliResult.VpsCreateResult(CliResult.OperationResult.from(operation), operation.vpsId().value()), out());
        return operation.status() == OperationStatus.FAILED ? ExitCodes.GENERAL_ERROR : ExitCodes.OK;
    }
}
