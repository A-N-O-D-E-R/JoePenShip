package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.vps.CreateVpsOperation;
import io.virtualization.sdk.vps.DataSize;
import io.virtualization.sdk.vps.NetworkConfiguration;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsManager;
import io.virtualization.sdk.vps.VpsSpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * {@code /api/v1/vps} — the provider-neutral VPS layer, over whichever single backend {@code
 * virtualization.vps.provider} names (see {@link io.virtualization.sdk.spring.VpsAutoConfiguration}).
 * Only registers when a {@link VpsManager} bean exists — no {@code provider} configured, no endpoints.
 *
 * <p>Every operation is exposed synchronously, same as {@link WorkloadController}/{@link
 * ImageController}: the controller awaits completion (bounded by {@link WebDefaults#OPERATION_TIMEOUT})
 * and returns the terminal state.
 */
@RestController
@RequestMapping("/api/v1/vps")
@ConditionalOnBean(VpsManager.class)
public class VpsController {

    private final VpsManager manager;

    public VpsController(VpsManager manager) {
        this.manager = manager;
    }

    @PostMapping
    public CreateVpsView create(@RequestBody CreateVpsRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        Objects.requireNonNull(request.image(), "'image' is required");

        CreateVpsOperation operation = manager.create(toSpec(request));
        operation.await(WebDefaults.OPERATION_TIMEOUT);
        return CreateVpsView.from(operation);
    }

    @GetMapping
    public List<VpsView> list() {
        return manager.list().stream().map(VpsView::from).toList();
    }

    @GetMapping("/{id}")
    public VpsView get(@PathVariable String id) {
        return VpsView.from(manager.get(new VpsId(id)));
    }

    @PostMapping("/{id}/start")
    public OperationView start(@PathVariable String id) {
        return awaited(manager.start(new VpsId(id)));
    }

    @PostMapping("/{id}/stop")
    public OperationView stop(@PathVariable String id) {
        return awaited(manager.stop(new VpsId(id)));
    }

    @PostMapping("/{id}/restart")
    public OperationView restart(@PathVariable String id) {
        return awaited(manager.restart(new VpsId(id)));
    }

    @PostMapping("/{id}/shutdown")
    public OperationView shutdown(@PathVariable String id) {
        return awaited(manager.shutdown(new VpsId(id)));
    }

    @DeleteMapping("/{id}")
    public OperationView destroy(@PathVariable String id) {
        return awaited(manager.destroy(new VpsId(id)));
    }

    @PostMapping("/{id}/rebuild")
    public CreateVpsView rebuild(@PathVariable String id, @RequestBody RebuildVpsRequestBody request) {
        Objects.requireNonNull(request.image(), "'image' is required");

        CreateVpsOperation operation = manager.rebuild(new VpsId(id), toImageReference(request.image()));
        operation.await(WebDefaults.OPERATION_TIMEOUT);
        return CreateVpsView.from(operation);
    }

    private OperationView awaited(Operation operation) {
        operation.await(WebDefaults.OPERATION_TIMEOUT);
        return OperationView.from(operation);
    }

    private static VpsSpec toSpec(CreateVpsRequestBody request) {
        VpsSpec.Builder builder = VpsSpec.builder(request.name(), toImageReference(request.image()));
        if (request.type() != null) {
            builder.type(request.type());
        }
        if (request.cpu() != null) {
            builder.cpu(request.cpu());
        }
        if (request.memoryMb() != null) {
            builder.memory(DataSize.ofMegabytes(request.memoryMb()));
        }
        if (request.diskMb() != null) {
            builder.disk(DataSize.ofMegabytes(request.diskMb()));
        }
        if (request.storagePool() != null) {
            builder.storagePool(request.storagePool());
        }
        if (request.volumeType() != null) {
            builder.volumeType(request.volumeType());
        }
        if (request.network() != null) {
            CreateVpsRequestBody.NetworkRequestBody network = request.network();
            builder.network(new NetworkConfiguration(network.network(), network.ipv4(), network.ipv6(), network.hostname()));
        }
        if (request.sshPublicKeys() != null) {
            builder.sshPublicKeys(request.sshPublicKeys());
        }
        if (request.cloudInit() != null) {
            builder.cloudInit(request.cloudInit());
        }
        if (request.metadata() != null) {
            builder.metadata(request.metadata());
        }
        if (request.labels() != null) {
            builder.labels(request.labels());
        }
        if (request.location() != null) {
            builder.location(request.location());
        }
        if (request.project() != null) {
            builder.project(request.project());
        }
        if (request.idempotencyKey() != null) {
            builder.idempotencyKey(request.idempotencyKey());
        }
        return builder.build();
    }

    private static ImageReference toImageReference(CreateVpsRequestBody.ImageRequestBody image) {
        Objects.requireNonNull(image.provider(), "'image.provider' is required");
        Objects.requireNonNull(image.name(), "'image.name' is required");
        return new ImageReference(image.provider(), image.remote(), image.name());
    }
}
