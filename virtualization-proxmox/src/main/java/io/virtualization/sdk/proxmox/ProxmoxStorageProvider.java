package io.virtualization.sdk.proxmox;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.proxmox.client.ProxmoxApiClient;
import io.virtualization.sdk.proxmox.internal.TaskPoller;
import io.virtualization.sdk.storage.StoragePool;
import io.virtualization.sdk.storage.StorageProvider;
import io.virtualization.sdk.storage.Volume;
import io.virtualization.sdk.storage.VolumeContentType;
import io.virtualization.sdk.storage.VolumeSnapshot;
import io.virtualization.sdk.storage.VolumeSpec;
import io.virtualization.sdk.storage.VolumeType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standalone volumes on a Proxmox storage (pool = Proxmox storage id). Proxmox ties every disk to
 * an owner VMID through its name ({@code vm-<vmid>-...}); these volumes are owned by
 * {@link #OWNER_VMID}, a VMID no VM gets, so that detaching a volume or destroying the VM it was
 * attached to never deletes it (verified on PVE 9.2). Only {@link #deleteVolume} frees it.
 *
 * <p>Limits: Proxmox resizes a disk only through the VM it is attached to; per-volume snapshots
 * are not available through the Proxmox API (Ceph RBD would need its own client). Volumes are
 * created on one node's view of the storage: use shared storage (Ceph, NFS) in a cluster.
 */
public final class ProxmoxStorageProvider implements StorageProvider {

    /** Owner of standalone volumes: the top of the VMID range, never handed out by /cluster/nextid in practice. */
    public static final int OWNER_VMID = 999_999_999;
    private static final String PREFIX = "vm-" + OWNER_VMID + "-";
    private static final Set<String> FILE_BASED = Set.of("dir", "nfs", "cifs", "glusterfs", "cephfs", "btrfs");
    private static final Pattern NAME = Pattern.compile("[a-z0-9][a-z0-9-]{0,62}");
    private static final Pattern SIZE = Pattern.compile("(\\d+)\\s*(G|GB|GiB)?");

    /** Where a volume is attached: VM, its node and the config slot. */
    record Attachment(String vmid, String node, String slot) {
    }

    private final ProxmoxApiClient client;
    private final String node;
    private final Duration taskPollInterval;

    /** @param node node whose storages are used; {@code null} = the first online node */
    public ProxmoxStorageProvider(ProxmoxClientConfig config, String node) {
        this(new ProxmoxApiClient(config), node, config.taskPollInterval());
    }

    ProxmoxStorageProvider(ProxmoxApiClient client, String node, Duration taskPollInterval) {
        this.client = client;
        this.node = node;
        this.taskPollInterval = taskPollInterval;
    }

    private String node() {
        if (node != null) {
            return node;
        }
        for (JsonNode n : client.getData("/nodes")) {
            if ("online".equals(n.path("status").asText())) {
                return n.path("node").asText();
            }
        }
        throw new OperationException("No online Proxmox node");
    }

    @Override
    public List<StoragePool> listStoragePools() {
        List<StoragePool> out = new ArrayList<>();
        for (JsonNode s : client.getData("/nodes/" + node() + "/storage")) {
            out.add(new StoragePool(s.path("storage").asText(), s.path("type").asText(), null,
                    Map.of("content", s.path("content").asText(""), "shared", s.path("shared").asText("0")),
                    s.path("active").asInt(0) == 1 ? "active" : "inactive"));
        }
        return out;
    }

    @Override
    public StoragePool getStoragePool(String name) {
        return listStoragePools().stream().filter(p -> p.name().equals(name)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No Proxmox storage '" + name + "'"));
    }

    @Override
    public List<Volume> listVolumes(String pool) {
        getStoragePool(pool);
        List<Volume> out = new ArrayList<>();
        for (JsonNode v : client.getData("/nodes/" + node() + "/storage/" + pool + "/content?content=images&vmid=" + OWNER_VMID)) {
            String volid = v.path("volid").asText();
            String file = volid.substring(volid.lastIndexOf('/') + 1).replaceFirst("^.*:", "");
            if (file.startsWith(PREFIX)) {
                String name = file.substring(PREFIX.length()).replaceFirst("\\.(raw|qcow2)$", "");
                long gib = v.path("size").asLong() / (1024L * 1024 * 1024);
                out.add(new Volume(pool, name, VolumeType.CUSTOM, VolumeContentType.BLOCK, gib + "G", Map.of("volid", volid)));
            }
        }
        return out;
    }

    @Override
    public Volume getVolume(String pool, String name) {
        return listVolumes(pool).stream().filter(v -> v.name().equals(name)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No volume '" + name + "' on storage '" + pool + "'"));
    }

    /** {@code spec.size} in GiB ({@code "10"}, {@code "10G"}, {@code "10GB"}, {@code "10GiB"}). */
    @Override
    public Volume createVolume(String pool, VolumeSpec spec) {
        if (!NAME.matcher(spec.name()).matches()) {
            throw new IllegalArgumentException("Volume name must match " + NAME.pattern() + ", got '" + spec.name() + "'");
        }
        long gib = gib(spec.size());
        StoragePool p = getStoragePool(pool);
        String file = PREFIX + spec.name() + (FILE_BASED.contains(p.driver()) ? ".raw" : "");
        awaitIfTask(client.postForm("/nodes/" + node() + "/storage/" + pool + "/content",
                Map.of("vmid", String.valueOf(OWNER_VMID), "filename", file, "size", gib + "G")));
        return getVolume(pool, spec.name());
    }

    @Override
    public void deleteVolume(String pool, String name) {
        String volid = volid(pool, name);
        attachment(client, volid).ifPresent(a -> {
            throw new OperationException("Volume " + volid + " is attached to VM " + a.vmid());
        });
        awaitIfTask(client.deleteData("/nodes/" + node() + "/storage/" + pool + "/content/" + URLEncoder.encode(volid, StandardCharsets.UTF_8)));
    }

    /** Grows an attached volume to {@code newSize} (GiB); Proxmox cannot resize a detached disk, nor shrink one. */
    @Override
    public Volume resizeVolume(String pool, String name, String newSize) {
        String volid = volid(pool, name);
        long gib = gib(newSize);
        Attachment a = attachment(client, volid).orElseThrow(() ->
                new OperationException("Proxmox resizes volumes only while attached to a VM: " + volid));
        awaitIfTask(client.putForm("/nodes/" + a.node() + "/qemu/" + a.vmid() + "/resize", Map.of("disk", a.slot(), "size", gib + "G")));
        return getVolume(pool, name);
    }

    @Override
    public List<VolumeSnapshot> listSnapshots(String pool, String volumeName) {
        throw noSnapshots();
    }

    @Override
    public VolumeSnapshot createSnapshot(String pool, String volumeName, String snapshotName) {
        throw noSnapshots();
    }

    @Override
    public void restoreSnapshot(String pool, String volumeName, String snapshotName) {
        throw noSnapshots();
    }

    @Override
    public void deleteSnapshot(String pool, String volumeName, String snapshotName) {
        throw noSnapshots();
    }

    private static UnsupportedCapabilityException noSnapshots() {
        return new UnsupportedCapabilityException("Proxmox volume snapshots are not supported (whole-VM snapshots only)");
    }

    private String volid(String pool, String name) {
        return getVolume(pool, name).config().get("volid");
    }

    private static long gib(String size) {
        Matcher m = SIZE.matcher(size == null ? "" : size.trim());
        if (!m.matches() || Long.parseLong(m.group(1)) < 1) {
            throw new IllegalArgumentException("Volume size must be a whole number of GiB, got '" + size + "'");
        }
        return Long.parseLong(m.group(1));
    }

    private void awaitIfTask(JsonNode data) {
        if (data != null && data.isTextual() && data.asText().startsWith("UPID:")) {
            String upid = data.asText();
            TaskPoller.await(client, upid.split(":")[1], upid, taskPollInterval);
        }
    }

    /**
     * The VM and slot a volume is attached to, by scanning VM configs.
     * ponytail: O(VMs) API calls; fine for small clusters, keep an index if it shows.
     */
    static Optional<Attachment> attachment(ProxmoxApiClient client, String volid) {
        for (JsonNode r : client.getData("/cluster/resources?type=vm")) {
            if (!"qemu".equals(r.path("type").asText()) || r.path("template").asInt(0) == 1) {
                continue;
            }
            String vmid = r.path("vmid").asText();
            String vmNode = r.path("node").asText();
            var slot = slotOf(client.getData("/nodes/" + vmNode + "/qemu/" + vmid + "/config"), volid);
            if (slot.isPresent()) {
                return Optional.of(new Attachment(vmid, vmNode, slot.get()));
            }
        }
        return Optional.empty();
    }

    /** The disk slot (scsiN, virtioN, sataN, ideN) holding {@code volid} in a VM config. */
    static Optional<String> slotOf(JsonNode config, String volid) {
        for (var e : config.properties()) {
            String v = e.getValue().asText("");
            if (e.getKey().matches("(scsi|virtio|sata|ide)\\d+") && (v.equals(volid) || v.startsWith(volid + ","))) {
                return Optional.of(e.getKey());
            }
        }
        return Optional.empty();
    }
}
