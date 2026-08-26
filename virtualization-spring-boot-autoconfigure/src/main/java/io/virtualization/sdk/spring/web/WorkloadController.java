package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ProviderOptions;
import io.virtualization.sdk.core.image.WorkloadSpec;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

/** {@code /api/v1/workloads} — creates a workload (container or VM) from an image. */
@RestController
@RequestMapping("/api/v1/workloads")
public class WorkloadController {

    private final VirtualizationClient client;

    public WorkloadController(VirtualizationClient client) {
        this.client = client;
    }

    @PostMapping
    public CreateWorkloadView create(@RequestBody CreateWorkloadRequestBody request) {
        Objects.requireNonNull(request.provider(), "'provider' is required");
        Objects.requireNonNull(request.name(), "'name' is required");
        Objects.requireNonNull(request.type(), "'type' is required");
        Objects.requireNonNull(request.image(), "'image' is required");

        ImageReference imageRef = new ImageReference(request.provider(), request.image().remote(), request.image().name());
        WorkloadSpec.Builder specBuilder = WorkloadSpec.builder(request.name(), request.type()).image(imageRef);
        if (request.cpu() != null || request.memoryMb() != null) {
            specBuilder.resources(new ComputeResources(
                    request.cpu() != null ? request.cpu() : 1, request.memoryMb() != null ? request.memoryMb() : 512));
        }
        if (request.storage() != null) {
            specBuilder.storage(request.storage());
        }
        if (request.networks() != null) {
            specBuilder.networks(request.networks());
        }
        if (request.environment() != null) {
            specBuilder.environment(request.environment());
        }
        if (request.providerOptions() != null && !request.providerOptions().isEmpty()) {
            specBuilder.providerOptions(ProviderOptions.of(request.providerOptions()));
        }

        CreateWorkloadOperation operation = client.provider(request.provider()).createFromImage(imageRef, specBuilder.build());
        operation.await(WebDefaults.OPERATION_TIMEOUT);
        return new CreateWorkloadView(OperationView.from(operation), operation.workloadId().orElse(null));
    }
}
