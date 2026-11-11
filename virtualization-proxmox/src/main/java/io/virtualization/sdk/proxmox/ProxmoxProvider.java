package io.virtualization.sdk.proxmox;

import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationHandle;
import io.virtualization.sdk.core.ProviderCapabilities;
import io.virtualization.sdk.core.ProviderType;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.proxmox.client.ProxmoxApiClient;
import io.virtualization.sdk.proxmox.client.dto.ClusterResourceDto;
import io.virtualization.sdk.proxmox.internal.DomainMapper;
import io.virtualization.sdk.proxmox.internal.TaskPoller;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.core.image.WorkloadType;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.firewall.WorkloadFirewall;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.core.image.CreateWorkloadHandle;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageAvailabilityPolicy;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * {@link VirtualizationProvider} backed by the Proxmox VE HTTPS REST API.
 *
 * <p>{@link #createFromImage} full-clones a QEMU template (the image identifier is the template's
 * VMID), then applies CPU/memory, cloud-init SSH keys ({@code configuration} key {@code
 * sshAuthorizedKeys}) and grows the boot disk to the {@code diskGb} provider option. The {@code node}
 * provider option picks the target node (default: the template's). {@code networks()} entries are
 * Proxmox ipconfig strings (cloud-init network per NIC). The VM is left stopped.
 *
 * <p>{@link #applyFirewall} uses the VM-level Proxmox firewall: it turns the firewall on for every
 * NIC, sets both default policies to DROP and replaces the VM's rules with ACCEPT rules. The
 * datacenter firewall must be enabled for VM firewalls to take effect.
 *
 * <p>Covers QEMU virtual machines only in this iteration; LXC containers, snapshots, storage and
 * networks are exposed by the Proxmox REST API and mapped by {@code virtualization-core}'s domain
 * model, but {@link VirtualizationProvider} itself has no methods for them yet — extending that
 * interface (or adding provider-specific accessor methods) is a follow-up, not implemented here to
 * avoid API surface with no caller.
 */
public final class ProxmoxProvider implements VirtualizationProvider {

    public static final ProviderType TYPE = new ProviderType("proxmox");

    private static final ProviderCapabilities CAPABILITIES = ProviderCapabilities.of(
            Capability.START, Capability.STOP, Capability.REBOOT, Capability.SHUTDOWN, Capability.DESTROY,
            Capability.FIREWALL, Capability.VOLUMES);
    private static final Pattern DATA_SLOT = Pattern.compile("scsi([1-9]|[12]\\d|30)");

    private static final Pattern DISK_SIZE = Pattern.compile("(?:^|,)size=(\\d+)([KMGT])");

    private final ProxmoxApiClient client;
    private final Duration taskPollInterval;

    public ProxmoxProvider(ProxmoxClientConfig config) {
        this(new ProxmoxApiClient(config), config.taskPollInterval());
    }

    /** Visible for tests, to inject a client talking to a fake server. */
    ProxmoxProvider(ProxmoxApiClient client, Duration taskPollInterval) {
        this.client = client;
        this.taskPollInterval = taskPollInterval;
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
        return client.getList("/cluster/resources?type=vm", ClusterResourceDto.class).stream()
                .filter(ClusterResourceDto::isQemuVm)
                .map(DomainMapper::toVirtualMachine)
                .toList();
    }

    /**
     * The VM with its live state. {@code /cluster/resources} is a cache refreshed every few
     * seconds (it can still say "stopped" right after a start), so the state comes from the VM's
     * own {@code status/current}. {@link #listVirtualMachines} keeps the cached view.
     */
    @Override
    public VirtualMachine getVirtualMachine(String id) {
        ClusterResourceDto r = findResource(id);
        String status = client.getData("/nodes/" + r.node() + "/qemu/" + id + "/status/current").path("status").asText(r.status());
        return DomainMapper.toVirtualMachine(new ClusterResourceDto(r.type(), r.node(), r.vmid(), r.name(), status, r.maxcpu(), r.maxmem()));
    }

    @Override
    public Operation start(String id) {
        return dispatch(id, "start", Capability.START);
    }

    @Override
    public Operation stop(String id) {
        return dispatch(id, "stop", Capability.STOP);
    }

    @Override
    public Operation reboot(String id) {
        return dispatch(id, "reboot", Capability.REBOOT);
    }

    @Override
    public Operation shutdown(String id) {
        return dispatch(id, "shutdown", Capability.SHUTDOWN);
    }

    @Override
    public Operation destroy(String id) {
        if (!CAPABILITIES.supports(Capability.DESTROY)) {
            throw new UnsupportedCapabilityException("Provider '" + TYPE.id() + "' does not support capability DESTROY");
        }
        ClusterResourceDto resource = findResource(id);
        String upid = client.deleteForTaskId("/nodes/" + resource.node() + "/qemu/" + id);
        return awaitedOperation(resource.node(), upid);
    }

    @Override
    public CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec, ImageAvailabilityPolicy policy) {
        if (spec.type() != WorkloadType.VIRTUAL_MACHINE) {
            throw new UnsupportedCapabilityException("Provider '" + TYPE.id() + "' only creates virtual machines");
        }
        if (!image.identifier().matches("\\d+")) {
            throw new IllegalArgumentException("Proxmox image identifier must be a template VMID, got '" + image.identifier() + "'");
        }
        for (String net : spec.networks()) {
            if (!net.matches("(ip|ip6)=[^\\s]+")) {
                throw new IllegalArgumentException("Networks must be Proxmox ipconfig strings (ip=<cidr>|dhcp[,gw=<ip>]), got '" + net + "'");
            }
        }
        // Proxmox templates cannot be pulled: every policy requires the template to exist.
        CreateWorkloadHandle handle = CreateWorkloadHandle.create("create-" + spec.name());
        Thread.ofVirtual().name(handle.operation().id()).start(() -> {
            try {
                handle.succeed(create(image.identifier(), spec, handle));
            } catch (VirtualizationException e) {
                handle.fail(e);
            } catch (RuntimeException e) {
                handle.fail(new OperationException("Creating '" + spec.name() + "' failed: " + e.getMessage(), e));
            }
        });
        return handle.operation();
    }

    /** {@code device}: a data slot {@code scsi1}..{@code scsi30} ({@code scsi0} is the boot disk). Hot-plugged on running VMs. */
    @Override
    public Operation attachVolume(String id, String volumeId, String device) {
        if (!DATA_SLOT.matcher(device).matches()) {
            throw new IllegalArgumentException("Device must be scsi1..scsi30, got '" + device + "'");
        }
        ClusterResourceDto vm = findResource(id);
        return async("attach-" + id + "-" + device, () -> {
            String base = "/nodes/" + vm.node() + "/qemu/" + id;
            JsonNode config = client.getData(base + "/config");
            if (volumeId.equals(config.path(device).asText("").split(",")[0])) {
                return; // already attached there
            }
            if (config.hasNonNull(device)) {
                throw new OperationException("Device " + device + " of VM " + id + " is in use");
            }
            ProxmoxStorageProvider.attachment(client, volumeId).ifPresent(a -> {
                throw new OperationException("Volume " + volumeId + " is attached to VM " + a.vmid());
            });
            awaitIfTask(vm.node(), client.postForm(base + "/config", Map.of(device, volumeId)));
        });
    }

    @Override
    public Operation detachVolume(String id, String volumeId) {
        ClusterResourceDto vm = findResource(id);
        return async("detach-" + id, () -> {
            String base = "/nodes/" + vm.node() + "/qemu/" + id;
            var slot = ProxmoxStorageProvider.slotOf(client.getData(base + "/config"), volumeId);
            if (slot.isPresent()) {
                // A volume owned by another VMID leaves the config entirely (no unusedN): its data stays.
                awaitIfTask(vm.node(), client.postForm(base + "/config", Map.of("delete", slot.get())));
            }
        });
    }

    private Operation async(String name, Runnable work) {
        OperationHandle handle = OperationHandle.create(name);
        Thread.ofVirtual().name(name).start(() -> {
            try {
                work.run();
                handle.complete();
            } catch (VirtualizationException e) {
                handle.fail(e);
            } catch (RuntimeException e) {
                handle.fail(new OperationException(name + " failed: " + e.getMessage(), e));
            }
        });
        return handle.operation();
    }

    @Override
    public Operation applyFirewall(String id, WorkloadFirewall firewall) {
        ClusterResourceDto vm = findResource(id);
        OperationHandle handle = OperationHandle.create("firewall-" + id);
        Thread.ofVirtual().name(handle.operation().id()).start(() -> {
            try {
                applyFirewall(vm.node(), id, firewall);
                handle.complete();
            } catch (VirtualizationException e) {
                handle.fail(e);
            } catch (RuntimeException e) {
                handle.fail(new OperationException("Applying the firewall of VM " + id + " failed: " + e.getMessage(), e));
            }
        });
        return handle.operation();
    }

    private void applyFirewall(String node, String vmid, WorkloadFirewall firewall) {
        String base = "/nodes/" + node + "/qemu/" + vmid;
        // Rules only apply to NICs with firewall=1.
        Map<String, String> nics = new LinkedHashMap<>();
        client.getData(base + "/config").properties().forEach(e -> {
            String value = e.getValue().asText();
            if (e.getKey().matches("net\\d+") && !value.contains("firewall=1")) {
                nics.put(e.getKey(), value.replace(",firewall=0", "") + ",firewall=1");
            }
        });
        if (!nics.isEmpty()) {
            awaitIfTask(node, client.postForm(base + "/config", nics));
        }
        client.putForm(base + "/firewall/options", Map.of("enable", "1", "policy_in", "DROP", "policy_out", "DROP"));

        // Replace the rules. Deleting shifts positions, so go from the last one up; meanwhile the
        // DROP policies deny everything (fails closed).
        List<Integer> positions = new java.util.ArrayList<>();
        client.getData(base + "/firewall/rules").forEach(r -> positions.add(r.path("pos").asInt()));
        positions.sort(java.util.Comparator.reverseOrder());
        for (int pos : positions) {
            client.deleteForTaskId(base + "/firewall/rules/" + pos);
        }
        for (WorkloadFirewall.Rule rule : firewall.rules()) {
            // Proxmox refuses a source/dest list mixing IPv4 and IPv6: one rule per family.
            Map<Boolean, List<String>> byFamily = rule.peers().stream().collect(Collectors.partitioningBy(p -> p.contains(":")));
            for (List<String> peers : byFamily.values()) {
                Map<String, String> form = peers.isEmpty() ? null : firewallRule(rule, peers);
                if (form != null) {
                    client.postForm(base + "/firewall/rules", form);
                }
            }
        }
    }

    /** Proxmox only takes ICMP types by name. */
    private static final Map<Integer, String> ICMP_TYPES = Map.ofEntries(
            Map.entry(0, "echo-reply"), Map.entry(3, "destination-unreachable"), Map.entry(4, "source-quench"),
            Map.entry(5, "redirect"), Map.entry(8, "echo-request"), Map.entry(9, "router-advertisement"),
            Map.entry(10, "router-solicitation"), Map.entry(11, "time-exceeded"), Map.entry(12, "parameter-problem"),
            Map.entry(13, "timestamp-request"), Map.entry(14, "timestamp-reply"));
    private static final Map<Integer, String> ICMPV6_TYPES = Map.ofEntries(
            Map.entry(1, "destination-unreachable"), Map.entry(2, "packet-too-big"), Map.entry(3, "time-exceeded"),
            Map.entry(4, "parameter-problem"), Map.entry(128, "echo-request"), Map.entry(129, "echo-reply"),
            Map.entry(133, "router-solicitation"), Map.entry(134, "router-advertisement"),
            Map.entry(135, "neighbour-solicitation"), Map.entry(136, "neighbour-advertisement"), Map.entry(137, "redirect"));

    /** The Proxmox rule, or {@code null} if it cannot be expressed (then it is left out: narrower, never wider). */
    static Map<String, String> firewallRule(WorkloadFirewall.Rule rule, List<String> peers) {
        Map<String, String> form = new LinkedHashMap<>();
        boolean in = rule.direction() == WorkloadFirewall.Direction.INBOUND;
        form.put("type", in ? "in" : "out");
        form.put("action", "ACCEPT");
        form.put("enable", "1");
        form.put(in ? "source" : "dest", String.join(",", peers));
        if (rule.protocol() != null) {
            form.put("proto", rule.protocol().equals("icmpv6") ? "ipv6-icmp" : rule.protocol());
        }
        boolean ports = "tcp".equals(rule.protocol()) || "udp".equals(rule.protocol());
        if (ports && rule.fromPort() != null && !(rule.fromPort() == 0 && rule.toPort() == 65535)) {
            form.put("dport", rule.fromPort().equals(rule.toPort()) ? String.valueOf(rule.fromPort()) : rule.fromPort() + ":" + rule.toPort());
        }
        if (rule.protocol() != null && rule.protocol().startsWith("icmp") && rule.fromPort() != null && rule.fromPort() >= 0) {
            String type = (rule.protocol().equals("icmp") ? ICMP_TYPES : ICMPV6_TYPES).get(rule.fromPort());
            if (type == null) {
                return null;
            }
            form.put("icmp-type", type);
        }
        if (rule.comment() != null) {
            form.put("comment", rule.comment());
        }
        return form;
    }

    private String create(String templateId, WorkloadSpec spec, CreateWorkloadHandle handle) {
        ClusterResourceDto template = findResource(templateId);
        String node = spec.providerOptions().getString("node").orElse(template.node());

        // Idempotent for retries: a VM already carrying the name is the one a previous attempt cloned.
        String vmid = findByName(spec.name()).map(vm -> String.valueOf(vm.vmid())).orElse(null);
        if (vmid == null) {
            vmid = cloneTemplate(template, node, spec.name());
        } else {
            node = findResource(vmid).node();
        }
        handle.updateProgress(0.6);

        Map<String, String> config = new LinkedHashMap<>();
        spec.resources().ifPresent(r -> {
            config.put("cores", String.valueOf(r.cpuCores()));
            config.put("memory", String.valueOf(r.memoryMb()));
        });
        // Cloud-init network: networks() entries in Proxmox's ipconfig format ("ip=10.0.0.5/24,gw=10.0.0.1"
        // or "ip=dhcp"), one per NIC in order; nameserver/searchdomain provider options.
        for (int i = 0; i < spec.networks().size(); i++) {
            config.put("ipconfig" + i, spec.networks().get(i));
        }
        spec.providerOptions().getString("nameserver").ifPresent(v -> config.put("nameserver", v));
        spec.providerOptions().getString("searchdomain").ifPresent(v -> config.put("searchdomain", v));
        String keys = spec.configuration().get("sshAuthorizedKeys");
        if (keys != null) {
            // Proxmox wants sshkeys URL-encoded (spaces as %20) inside the form-encoded body.
            config.put("sshkeys", URLEncoder.encode(keys.strip() + "\n", StandardCharsets.UTF_8).replace("+", "%20"));
        }
        if (!spec.labels().isEmpty()) {
            config.put("description", spec.labels().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue()).sorted().collect(Collectors.joining("\n")));
        }
        if (!config.isEmpty()) {
            awaitIfTask(node, client.postForm("/nodes/" + node + "/qemu/" + vmid + "/config", config));
        }
        handle.updateProgress(0.8);

        Optional<Object> diskGb = spec.providerOptions().get("diskGb");
        if (diskGb.isPresent()) {
            growBootDisk(node, vmid, Long.parseLong(diskGb.get().toString()));
        }
        return vmid;
    }

    private String cloneTemplate(ClusterResourceDto template, String node, String name) {
        // nextid can race with another create: Proxmox then refuses the clone, so retry with a fresh id.
        for (int attempt = 1; ; attempt++) {
            String vmid = client.getData("/cluster/nextid").asText();
            if (vmid.equals(String.valueOf(ProxmoxStorageProvider.OWNER_VMID))) {
                // That VMID owns every standalone volume: a VM there would delete them all when destroyed.
                throw new OperationException("Refusing VMID " + vmid + ", reserved for standalone volumes");
            }
            Map<String, String> form = new LinkedHashMap<>();
            form.put("newid", vmid);
            form.put("name", name);
            form.put("full", "1");
            if (!node.equals(template.node())) {
                form.put("target", node);
            }
            try {
                String upid = client.postForm("/nodes/" + template.node() + "/qemu/" + template.vmid() + "/clone", form).asText();
                TaskPoller.await(client, template.node(), upid, taskPollInterval);
                return vmid;
            } catch (OperationException e) {
                if (attempt >= 3 || e.getMessage() == null || !e.getMessage().contains("already exists")) {
                    throw e;
                }
            }
        }
    }

    /** Grows the boot disk to {@code gb}; never shrinks (Proxmox refuses that). */
    private void growBootDisk(String node, String vmid, long gb) {
        JsonNode config = client.getData("/nodes/" + node + "/qemu/" + vmid + "/config");
        String disk = bootDisk(config);
        Matcher size = DISK_SIZE.matcher(config.path(disk).asText(""));
        long currentGb = size.find() ? toGb(Long.parseLong(size.group(1)), size.group(2)) : 0;
        if (gb > currentGb) {
            awaitIfTask(node, client.putForm("/nodes/" + node + "/qemu/" + vmid + "/resize", Map.of("disk", disk, "size", gb + "G")));
        }
    }

    /** First disk of the boot order ({@code order=scsi0;ide2;net0}), skipping CD-ROMs; else the first usual disk. */
    static String bootDisk(JsonNode config) {
        String boot = config.path("boot").asText("");
        if (boot.startsWith("order=")) {
            for (String dev : boot.substring(6).split(";")) {
                String value = config.path(dev).asText("");
                if (!value.isEmpty() && !value.contains("media=cdrom")) {
                    return dev;
                }
            }
        }
        for (String dev : List.of("scsi0", "virtio0", "sata0", "ide0")) {
            if (config.hasNonNull(dev)) {
                return dev;
            }
        }
        throw new OperationException("No boot disk found in the VM configuration");
    }

    private static long toGb(long value, String unit) {
        return switch (unit) {
            case "T" -> value * 1024;
            case "G" -> value;
            case "M" -> Math.ceilDiv(value, 1024);
            default -> Math.ceilDiv(value, 1024 * 1024); // K
        };
    }

    /** Some endpoints answer synchronously ({@code null}), others with a task UPID depending on the PVE version. */
    private void awaitIfTask(String node, JsonNode data) {
        if (data != null && data.isTextual() && data.asText().startsWith("UPID:")) {
            TaskPoller.await(client, node, data.asText(), taskPollInterval);
        }
    }

    private Optional<ClusterResourceDto> findByName(String name) {
        return client.getList("/cluster/resources?type=vm", ClusterResourceDto.class).stream()
                .filter(ClusterResourceDto::isQemuVm)
                .filter(dto -> name.equals(dto.name()))
                .findFirst();
    }

    private Operation dispatch(String id, String action, Capability required) {
        if (!CAPABILITIES.supports(required)) {
            throw new UnsupportedCapabilityException("Provider '" + TYPE.id() + "' does not support capability " + required);
        }
        ClusterResourceDto resource = findResource(id);
        String upid = client.postForTaskId("/nodes/" + resource.node() + "/qemu/" + id + "/status/" + action);
        return awaitedOperation(resource.node(), upid);
    }

    private Operation awaitedOperation(String node, String upid) {
        OperationHandle handle = OperationHandle.create(upid);
        TaskPoller.pollAsync(client, node, upid, handle, taskPollInterval);
        return handle.operation();
    }

    private ClusterResourceDto findResource(String id) {
        return client.getList("/cluster/resources?type=vm", ClusterResourceDto.class).stream()
                .filter(ClusterResourceDto::isQemuVm)
                .filter(dto -> id.equals(String.valueOf(dto.vmid())))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No Proxmox QEMU VM with id '" + id + "'"));
    }
}
