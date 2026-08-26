package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ProviderOptions;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Command(name = "create", description = "Create a workload (container or VM) from an image.")
final class WorkloadCreateCommand extends AbstractCliCommand {

    @Option(names = "--image", required = true, paramLabel = "<reference>", description = "Image reference, e.g. images:ubuntu/24.04.")
    String image;

    @Option(names = "--name", required = true, description = "Name for the new workload.")
    String name;

    @Option(names = "--type", defaultValue = "CONTAINER", description = "Workload type: ${COMPLETION-CANDIDATES} (default: CONTAINER).")
    WorkloadType type;

    @Option(names = "--cpu", description = "Number of CPU cores.")
    Integer cpu;

    @Option(names = "--memory", description = "Memory in megabytes.")
    Long memoryMb;

    @Option(names = "--storage", description = "Storage specifier, repeatable.")
    List<String> storage = List.of();

    @Option(names = "--network", description = "Network to attach, repeatable.")
    List<String> networks = List.of();

    @Option(names = "--profile", description = "Provider profile to apply (e.g. an Incus profile).")
    String profile;

    @Option(names = "--project", description = "Provider project (not yet supported by any provider).")
    String project;

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

        WorkloadSpec.Builder specBuilder = WorkloadSpec.builder(name, type).image(ref).storage(storage).networks(networks);
        if (cpu != null || memoryMb != null) {
            specBuilder.resources(new ComputeResources(cpu != null ? cpu : 1, memoryMb != null ? memoryMb : 512));
        }
        Map<String, Object> providerOptions = new LinkedHashMap<>();
        if (profile != null) {
            providerOptions.put("profile", profile);
        }
        if (project != null) {
            providerOptions.put("project", project);
        }
        if (!providerOptions.isEmpty()) {
            specBuilder.providerOptions(ProviderOptions.of(providerOptions));
        }

        CreateWorkloadOperation operation = provider().createFromImage(ref, specBuilder.build());
        if (wait) {
            operation.await(Duration.ofSeconds(timeoutSeconds));
        }
        outputWriter().write(
                new CliResult.WorkloadCreateResult(CliResult.OperationResult.from(operation), operation.workloadId().orElse(null)),
                out());
        return operation.status() == OperationStatus.FAILED ? ExitCodes.GENERAL_ERROR : ExitCodes.OK;
    }
}
