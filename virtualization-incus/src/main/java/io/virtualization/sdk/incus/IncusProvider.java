package io.virtualization.sdk.incus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.Container;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.ProviderCapabilities;
import io.virtualization.sdk.core.ProviderType;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.CreateWorkloadHandle;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageAvailabilityPolicy;
import io.virtualization.sdk.core.image.ImagePullOperation;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.client.dto.InstanceDto;
import io.virtualization.sdk.incus.internal.DomainMapper;
import io.virtualization.sdk.incus.internal.ImageOperationWaiter;
import io.virtualization.sdk.incus.internal.OperationWaiter;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link VirtualizationProvider} backed by the Incus REST API over mutual TLS.
 *
 * <p>Covers both Incus instance types: {@link #listVirtualMachines()}/{@link
 * #getVirtualMachine(String)} for {@code virtual-machine} instances, {@link #listContainers()}/
 * {@link #getContainer(String)} for {@code container} instances. The plain lifecycle methods
 * ({@code start}/{@code stop}/{@code reboot}/{@code shutdown}/{@code destroy}) are id-only and
 * type-agnostic — they operate on either kind of instance. Profiles, networks, storage and
 * projects are exposed by the Incus REST API but {@link VirtualizationProvider} has no methods for
 * them yet. {@link #createFromImage} supports both container and VM creation via {@link
 * WorkloadSpec#type()}, and applies {@code cloud-init.user-data} from {@code
 * WorkloadSpec#providerOptions()}'s {@code "cloudInit"}/{@code "sshPublicKeys"}/{@code "hostname"}
 * keys, and builds Incus {@code devices} overrides from {@link WorkloadSpec#storage()}/{@link
 * WorkloadSpec#networks()} plus the {@code "ipv4"}/{@code "ipv6"}/{@code "storagePool"}
 * providerOptions — see {@link #buildCreateBody}. {@code "volumeType"}, {@code "location"} and
 * {@code "project"} providerOptions still aren't applied — no direct Incus device/target
 * equivalent wired up yet.
 */
public final class IncusProvider implements VirtualizationProvider {

    public static final ProviderType TYPE = new ProviderType("incus");

    private static final ProviderCapabilities CAPABILITIES = ProviderCapabilities.of(
            Capability.START, Capability.STOP, Capability.REBOOT, Capability.SHUTDOWN, Capability.DESTROY);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final IncusApiClient client;
    private final Duration operationWaitTimeout;
    private final IncusImageProvider images;

    public IncusProvider(IncusClientConfig config) {
        this(new IncusApiClient(config), config.operationWaitTimeout(), new IncusImageProvider(config));
    }

    /** Visible for tests, to inject a client talking to a fake server. */
    IncusProvider(IncusApiClient client, Duration operationWaitTimeout) {
        this(client, operationWaitTimeout, new IncusImageProvider(client, "local"));
    }

    IncusProvider(IncusApiClient client, Duration operationWaitTimeout, IncusImageProvider images) {
        this.client = client;
        this.operationWaitTimeout = operationWaitTimeout;
        this.images = images;
    }

    @Override
    public ProviderType type() {
        return TYPE;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public List<VirtualMachine> listVirtualMachines() {
        return client.getList("/instances?recursion=1", InstanceDto.class).stream()
                .filter(InstanceDto::isVirtualMachine)
                .map(DomainMapper::toVirtualMachine)
                .toList();
    }

    @Override
    public VirtualMachine getVirtualMachine(String id) {
        InstanceDto instance = client.getSingle("/instances/" + id, InstanceDto.class);
        if (!instance.isVirtualMachine()) {
            throw new ResourceNotFoundException("No Incus virtual machine with id '" + id + "'");
        }
        return DomainMapper.toVirtualMachine(instance);
    }

    @Override
    public List<Container> listContainers() {
        return client.getList("/instances?recursion=1", InstanceDto.class).stream()
                .filter(InstanceDto::isContainer)
                .map(DomainMapper::toContainer)
                .toList();
    }

    @Override
    public Container getContainer(String id) {
        InstanceDto instance = client.getSingle("/instances/" + id, InstanceDto.class);
        if (!instance.isContainer()) {
            throw new ResourceNotFoundException("No Incus container with id '" + id + "'");
        }
        return DomainMapper.toContainer(instance);
    }

    @Override
    public Operation start(String id) {
        return dispatchStateAction(id, "start", Capability.START);
    }

    @Override
    public Operation stop(String id) {
        return dispatchStateAction(id, "stop", Capability.STOP);
    }

    @Override
    public Operation reboot(String id) {
        return dispatchStateAction(id, "restart", Capability.REBOOT);
    }

    @Override
    public Operation shutdown(String id) {
        return dispatchStateAction(id, "stop", Capability.SHUTDOWN);
    }

    @Override
    public Operation destroy(String id) {
        requireCapability(Capability.DESTROY);
        String operationId = client.deleteForOperationId("/instances/" + id);
        return awaitedOperation(operationId);
    }

    private Operation dispatchStateAction(String id, String action, Capability required) {
        requireCapability(required);
        String operationId = client.putForOperationId("/instances/" + id + "/state", "{\"action\":\"" + action + "\"}");
        return awaitedOperation(operationId);
    }

    private Operation awaitedOperation(String operationId) {
        OperationHandle handle = OperationHandle.create(operationId != null ? operationId : "sync");
        if (operationId == null) {
            handle.complete();
        } else {
            OperationWaiter.waitAsync(client, operationId, handle, operationWaitTimeout);
        }
        return handle.operation();
    }

    private void requireCapability(Capability required) {
        if (!CAPABILITIES.supports(required)) {
            throw new UnsupportedCapabilityException("Provider '" + TYPE.id() + "' does not support capability " + required);
        }
    }

    /**
     * Resolves {@code image} against {@code policy} (pulling it first if the policy calls for it),
     * then creates an Incus instance from it via {@code POST /instances}. Runs on a virtual thread
     * — this method returns as soon as the request is submitted.
     */
    @Override
    public CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec, ImageAvailabilityPolicy policy) {
        CreateWorkloadHandle handle = CreateWorkloadHandle.create("create-" + UUID.randomUUID());
        Thread.ofVirtual().name(handle.operation().id()).start(() -> runCreate(image, spec, policy, handle));
        return handle.operation();
    }

    private void runCreate(ImageReference image, WorkloadSpec spec, ImageAvailabilityPolicy policy, CreateWorkloadHandle handle) {
        try {
            InstanceSourceDto source = resolveSource(image, policy);
            String jsonBody = buildCreateBody(spec, source);
            String operationId = client.postForOperationId("/instances", jsonBody);
            if (operationId == null) {
                handle.succeed(spec.name());
                return;
            }
            ImageOperationWaiter.waitCreate(client, operationId, handle, spec.name(), operationWaitTimeout);
        } catch (VirtualizationException e) {
            handle.fail(e);
        }
    }

    /**
     * Resolves an {@link ImageReference} to the Incus {@code source} object an instance-create
     * request needs, applying {@code policy}:
     *
     * <ul>
     *   <li>same remote as {@code images}: the image must already exist locally regardless of
     *       policy — there is nowhere else to pull it from.
     *   <li>a different, known remote, {@link ImageAvailabilityPolicy#REQUIRE_LOCAL}: rejected,
     *       since by definition it isn't local.
     *   <li>{@link ImageAvailabilityPolicy#PULL_IF_MISSING}: the source references the remote
     *       server directly and Incus fetches-or-reuses-cache during creation itself.
     *   <li>{@link ImageAvailabilityPolicy#ALWAYS_REFRESH}: pulls explicitly first (via {@link
     *       IncusImageProvider#pull}), then creates from the freshly pulled fingerprint.
     * </ul>
     */
    private InstanceSourceDto resolveSource(ImageReference image, ImageAvailabilityPolicy policy) {
        boolean sameRemote = image.remote() == null || image.remote().equals(images.remote());
        if (sameRemote) {
            Image resolved = images.get(image).orElseThrow(() -> new ResourceNotFoundException(
                    "No image '" + image.identifier() + "' on remote '" + images.remote() + "'"));
            return InstanceSourceDto.fingerprint(resolved.id().value());
        }

        if (policy == ImageAvailabilityPolicy.REQUIRE_LOCAL) {
            throw new ResourceNotFoundException(
                    "Image '" + image.identifier() + "' on remote '" + image.remote() + "' is not local, and REQUIRE_LOCAL forbids pulling");
        }
        IncusRemote remoteServer = images.remotes().get(image.remote());
        if (remoteServer == null) {
            throw new UnsupportedCapabilityException("Unknown Incus remote '" + image.remote() + "'");
        }

        if (policy == ImageAvailabilityPolicy.ALWAYS_REFRESH) {
            ImagePullOperation pull = images.pull(image);
            if (pull.await(operationWaitTimeout) != OperationStatus.SUCCEEDED) {
                throw pull.error().orElseGet(() -> new OperationException("Pull failed for " + image));
            }
            ImageReference localRef = new ImageReference(images.name(), images.remote(), image.identifier());
            Image refreshed = images.get(localRef)
                    .orElseThrow(() -> new ResourceNotFoundException("Pulled image not found: " + image));
            return InstanceSourceDto.fingerprint(refreshed.id().value());
        }

        // PULL_IF_MISSING: let Incus fetch-or-reuse-cache during instance creation itself.
        return InstanceSourceDto.remote(image.identifier(), remoteServer.server().toString(), remoteServer.protocol());
    }

    private String buildCreateBody(WorkloadSpec spec, InstanceSourceDto source) {
        Map<String, String> config = new LinkedHashMap<>(spec.configuration());
        spec.resources().ifPresent(resources -> {
            config.put("limits.cpu", String.valueOf(resources.cpuCores()));
            config.put("limits.memory", resources.memoryMb() + "MB");
        });
        applyCloudInit(spec, config);
        List<String> profiles = spec.providerOptions().getString("profile").map(List::of).orElse(List.of("default"));
        String type = spec.type() == WorkloadType.VIRTUAL_MACHINE ? "virtual-machine" : "container";
        Map<String, Object> devices = buildDevices(spec);

        try {
            return MAPPER.writeValueAsString(new InstanceCreateDto(
                    spec.name(), spec.architecture().orElse(null), profiles, config, type, source, devices));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unreachable: instance create body always serializes", e);
        }
    }

    /**
     * Injects {@code cloud-init.user-data} from {@code providerOptions}, matching the key
     * conventions {@code virtualization-vps}'s {@code VpsProviderOptionKeys} documents — this
     * module has no dependency on that one, matched by string literal, same as {@code "profile"}
     * above:
     *
     * <ul>
     *   <li>{@code "cloudInit"} (a raw {@code #cloud-config} document, {@code String}): used
     *       verbatim as {@code cloud-init.user-data}. Takes priority — no attempt is made to merge
     *       {@code "sshPublicKeys"}/{@code "hostname"} into a caller-supplied document.
     *   <li>otherwise, {@code "sshPublicKeys"} ({@code List<String>}) and/or {@code "hostname"}
     *       ({@code String}): synthesizes a minimal {@code #cloud-config} document.
     * </ul>
     *
     * If none are present, no {@code cloud-init.user-data} key is set — the image's own defaults
     * apply. Values are YAML-quoted: they're caller-controlled and land in a document cloud-init
     * runs with root privilege inside the guest, so unescaped embedding would let a crafted
     * hostname/key break out of its scalar and inject arbitrary cloud-init directives.
     *
     * <p>Static addressing ({@code "ipv4"}/{@code "ipv6"}) and root-disk placement ({@code
     * "storagePool"}/{@code "volumeType"}) need an Incus {@code devices} entry this instance-
     * create body doesn't build yet — a known gap, not silently applied.
     */
    private static void applyCloudInit(WorkloadSpec spec, Map<String, String> config) {
        Optional<String> explicitCloudInit = spec.providerOptions().getString("cloudInit");
        if (explicitCloudInit.isPresent()) {
            config.put("cloud-init.user-data", explicitCloudInit.get());
            return;
        }
        List<String> sshPublicKeys = sshPublicKeys(spec);
        Optional<String> hostname = spec.providerOptions().getString("hostname");
        if (sshPublicKeys.isEmpty() && hostname.isEmpty()) {
            return;
        }
        config.put("cloud-init.user-data", synthesizeCloudConfig(sshPublicKeys, hostname.orElse(null)));
    }

    private static List<String> sshPublicKeys(WorkloadSpec spec) {
        Object value = spec.providerOptions().get("sshPublicKeys").orElse(null);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }

    private static String synthesizeCloudConfig(List<String> sshPublicKeys, String hostname) {
        StringBuilder yaml = new StringBuilder("#cloud-config\n");
        if (hostname != null) {
            yaml.append("hostname: ").append(yamlQuote(hostname)).append('\n');
            yaml.append("fqdn: ").append(yamlQuote(hostname)).append('\n');
        }
        if (!sshPublicKeys.isEmpty()) {
            yaml.append("ssh_authorized_keys:\n");
            for (String key : sshPublicKeys) {
                yaml.append("  - ").append(yamlQuote(key)).append('\n');
            }
        }
        return yaml.toString();
    }

    private static String yamlQuote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    /**
     * Builds {@code devices} overrides from {@link WorkloadSpec#storage()} (free-form {@code
     * "name:size"} entries, e.g. {@code "root:40960MB"}) and {@link WorkloadSpec#networks()} (a
     * single network name), plus the {@code "ipv4"}/{@code "ipv6"}/{@code "storagePool"}
     * providerOptions. An empty map means "no overrides" — the profile's own devices apply.
     */
    private static Map<String, Object> buildDevices(WorkloadSpec spec) {
        Map<String, Object> devices = new LinkedHashMap<>();
        applyStorageDevices(spec, devices);
        applyNetworkDevice(spec, devices);
        return devices;
    }

    private static void applyStorageDevices(WorkloadSpec spec, Map<String, Object> devices) {
        Optional<String> storagePool = spec.providerOptions().getString("storagePool");
        for (String entry : spec.storage()) {
            int colon = entry.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String name = entry.substring(0, colon);
            String size = entry.substring(colon + 1);
            Map<String, Object> device = new LinkedHashMap<>();
            device.put("type", "disk");
            device.put("path", name.equals("root") ? "/" : "/" + name);
            device.put("size", size);
            storagePool.ifPresent(pool -> device.put("pool", pool));
            devices.put(name, device);
        }
    }

    private static void applyNetworkDevice(WorkloadSpec spec, Map<String, Object> devices) {
        if (spec.networks().isEmpty()) {
            return;
        }
        Map<String, Object> device = new LinkedHashMap<>();
        device.put("type", "nic");
        device.put("network", spec.networks().get(0));
        spec.providerOptions().getString("ipv4").ifPresent(ip -> device.put("ipv4.address", ip));
        spec.providerOptions().getString("ipv6").ifPresent(ip -> device.put("ipv6.address", ip));
        devices.put("eth0", device);
    }

    private record InstanceSourceDto(String type, String fingerprint, String alias, String server, String protocol) {
        static InstanceSourceDto fingerprint(String fingerprint) {
            return new InstanceSourceDto("image", fingerprint, null, null, null);
        }

        static InstanceSourceDto remote(String alias, String server, String protocol) {
            return new InstanceSourceDto("image", null, alias, server, protocol);
        }
    }

    private record InstanceCreateDto(
            String name, String architecture, List<String> profiles, Map<String, String> config, String type,
            InstanceSourceDto source, Map<String, Object> devices) {}
}
