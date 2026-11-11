package io.virtualization.sdk.proxmox.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal in-process stand-in for the Proxmox VE REST API, built on the JDK's built-in {@link
 * HttpServer} (no extra test dependency). Supports exactly what {@code ProxmoxProvider} and {@code
 * ProxmoxApiClient} need: token auth, {@code /cluster/resources}, VM lifecycle actions and task
 * status polling.
 */
public final class FakeProxmoxServer implements AutoCloseable {

    private static final Pattern ACTION_PATH =
            Pattern.compile("/api2/json/nodes/([^/]+)/qemu/(\\d+)/status/([a-z]+)");
    private static final Pattern DESTROY_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/qemu/(\\d+)");
    private static final Pattern CLONE_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/qemu/(\\d+)/clone");
    private static final Pattern CONFIG_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/qemu/(\\d+)/config");
    private static final Pattern RESIZE_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/qemu/(\\d+)/resize");
    private static final Pattern FW_RULES_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/qemu/(\\d+)/firewall/rules(?:/(\\d+))?");
    private static final Pattern STATUS_CURRENT_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/qemu/(\\d+)/status/current");
    private static final Pattern FW_OPTIONS_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/qemu/(\\d+)/firewall/options");
    private static final Pattern TASK_STATUS_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/tasks/(.+)/status");

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpServer server;
    private final String expectedAuthorizationHeader;
    private final Map<Integer, VmState> vms = new ConcurrentHashMap<>();
    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private volatile int pollsBeforeCompletion = 0;
    private volatile boolean nextTaskFails = false;
    private final Map<Integer, Map<String, String>> configs = new ConcurrentHashMap<>();
    /** What /cluster/resources reports instead of the real status (Proxmox caches it for seconds). */
    private final Map<Integer, String> cachedStatus = new ConcurrentHashMap<>();
    private final Map<Integer, List<Map<String, String>>> firewallRules = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, String>> firewallOptions = new ConcurrentHashMap<>();
    private final java.util.concurrent.atomic.AtomicInteger nextIdCollisions = new java.util.concurrent.atomic.AtomicInteger();

    public FakeProxmoxServer(String expectedAuthorizationHeader) {
        this.expectedAuthorizationHeader = expectedAuthorizationHeader;
        try {
            this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to bind fake Proxmox server", e);
        }
        server.createContext("/api2/json/", this::handle);
    }

    public FakeProxmoxServer start() {
        server.start();
        return this;
    }

    public URI uri() {
        return URI.create("http://localhost:" + server.getAddress().getPort());
    }

    /** Number of poll requests that return "running" before a task finishes. 0 = finishes on first poll. */
    public void pollsBeforeCompletion(int polls) {
        this.pollsBeforeCompletion = polls;
    }

    /** The next task created will finish with a failure exit status. */
    public void nextTaskFails() {
        this.nextTaskFails = true;
    }

    public void addVm(int vmid, String node, String name, String status, int maxcpu, long maxmemBytes) {
        vms.put(vmid, new VmState(node, name, status, maxcpu, maxmemBytes));
    }

    /** A template VM with a boot disk of the given size (e.g. {@code "10G"}) and a cloud-init drive. */
    public void addTemplate(int vmid, String node, String name, String diskSize) {
        addVm(vmid, node, name, "stopped", 1, 1_073_741_824L);
        Map<String, String> config = new ConcurrentHashMap<>();
        config.put("boot", "order=ide2;scsi0;net0");
        config.put("ide2", "local-lvm:vm-" + vmid + "-cloudinit,media=cdrom");
        config.put("scsi0", "local-lvm:base-" + vmid + "-disk-0,size=" + diskSize);
        config.put("template", "1");
        config.put("net0", "virtio=BC:24:11:00:00:01,bridge=vmbr0");
        configs.put(vmid, config);
    }

    /** Makes /cluster/resources report {@code status} for the VM, while status/current stays truthful. */
    public void staleClusterStatus(int vmid, String status) {
        cachedStatus.put(vmid, status);
    }

    /** The VM firewall rules, top first (Proxmox inserts new rules at position 0). */
    public List<Map<String, String>> firewallRules(int vmid) {
        return firewallRules.getOrDefault(vmid, List.of());
    }

    public Map<String, String> firewallOptions(int vmid) {
        return firewallOptions.getOrDefault(vmid, Map.of());
    }

    /** The VM's configuration as last written (cloned from its template, then updated). */
    public Map<String, String> config(int vmid) {
        return configs.get(vmid);
    }

    public Integer vmidByName(String name) {
        return vms.entrySet().stream().filter(e -> e.getValue().name().equals(name)).map(Map.Entry::getKey).findFirst().orElse(null);
    }

    /** The next {@code times} calls to /cluster/nextid return an id already in use, like a race with another client. */
    public void nextIdCollides(int times) {
        nextIdCollisions.set(times);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!expectedAuthorizationHeader.equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
                sendJson(exchange, 401, Map.of());
                return;
            }
            route(exchange);
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method) && path.equals("/api2/json/cluster/resources")) {
            List<Map<String, Object>> resources = new ArrayList<>();
            vms.forEach((vmid, vm) -> resources.add(toResourceJson(vmid,
                    cachedStatus.containsKey(vmid) ? new VmState(vm.node(), vm.name(), cachedStatus.get(vmid), vm.maxcpu(), vm.maxmemBytes()) : vm)));
            sendJson(exchange, 200, Map.of("data", resources));
            return;
        }

        if ("GET".equals(method) && path.equals("/api2/json/cluster/nextid")) {
            int max = vms.keySet().stream().max(Integer::compare).orElse(99);
            int id = nextIdCollisions.getAndUpdate(n -> Math.max(0, n - 1)) > 0 ? max : max + 1;
            sendJson(exchange, 200, Map.of("data", String.valueOf(id)));
            return;
        }

        Matcher clone = CLONE_PATH.matcher(path);
        if ("POST".equals(method) && clone.matches()) {
            Map<String, String> form = form(exchange);
            int newid = Integer.parseInt(form.get("newid"));
            int source = Integer.parseInt(clone.group(2));
            if (vms.containsKey(newid)) {
                sendJson(exchange, 500, Map.of("message", "unable to create VM " + newid + ": config file already exists", "data", ""));
                return;
            }
            VmState template = vms.get(source);
            String target = form.getOrDefault("target", template.node());
            vms.put(newid, new VmState(target, form.get("name"), "stopped", template.maxcpu(), template.maxmemBytes()));
            Map<String, String> config = new ConcurrentHashMap<>(configs.getOrDefault(source, Map.of()));
            config.remove("template");
            config.replaceAll((k, v) -> v.replace("base-" + source, "vm-" + newid).replace("vm-" + source, "vm-" + newid));
            configs.put(newid, config);
            sendTask(exchange, clone.group(1), "qmclone", source);
            return;
        }

        Matcher config = CONFIG_PATH.matcher(path);
        if (config.matches()) {
            int vmid = Integer.parseInt(config.group(2));
            if (!vms.containsKey(vmid)) {
                sendJson(exchange, 404, Map.of());
                return;
            }
            if ("GET".equals(method)) {
                sendJson(exchange, 200, Map.of("data", configs.getOrDefault(vmid, Map.of())));
                return;
            }
            Map<String, String> form = form(exchange);
            configs.computeIfAbsent(vmid, k -> new ConcurrentHashMap<>()).putAll(form);
            if (form.containsKey("cores") || form.containsKey("memory")) {
                VmState vm = vms.get(vmid);
                vms.put(vmid, new VmState(vm.node(), vm.name(), vm.status(),
                        Integer.parseInt(form.getOrDefault("cores", String.valueOf(vm.maxcpu()))),
                        form.containsKey("memory") ? Long.parseLong(form.get("memory")) * 1024 * 1024 : vm.maxmemBytes()));
            }
            sendTask(exchange, config.group(1), "qmconfig", vmid); // async variant (POST)
            return;
        }

        Matcher current = STATUS_CURRENT_PATH.matcher(path);
        if ("GET".equals(method) && current.matches()) {
            VmState vm = vms.get(Integer.parseInt(current.group(2)));
            sendJson(exchange, vm == null ? 404 : 200, vm == null ? Map.of() : Map.of("data", Map.of("status", vm.status())));
            return;
        }

        Matcher fwRules = FW_RULES_PATH.matcher(path);
        if (fwRules.matches()) {
            int vmid = Integer.parseInt(fwRules.group(2));
            List<Map<String, String>> rules = firewallRules.computeIfAbsent(vmid, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
            if ("GET".equals(method)) {
                List<Map<String, Object>> out = new ArrayList<>();
                for (int i = 0; i < rules.size(); i++) {
                    Map<String, Object> r = new LinkedHashMap<>(rules.get(i));
                    r.put("pos", i);
                    out.add(r);
                }
                sendJson(exchange, 200, Map.of("data", out));
            } else if ("POST".equals(method)) {
                rules.addFirst(form(exchange));
                sendJson(exchange, 200, Map.of("data", ""));
            } else if ("DELETE".equals(method) && fwRules.group(3) != null) {
                rules.remove(Integer.parseInt(fwRules.group(3)));
                sendJson(exchange, 200, Map.of("data", ""));
            } else {
                sendJson(exchange, 404, Map.of());
            }
            return;
        }

        Matcher fwOptions = FW_OPTIONS_PATH.matcher(path);
        if ("PUT".equals(method) && fwOptions.matches()) {
            firewallOptions.computeIfAbsent(Integer.parseInt(fwOptions.group(2)), k -> new ConcurrentHashMap<>()).putAll(form(exchange));
            sendJson(exchange, 200, Map.of("data", ""));
            return;
        }

        Matcher resize = RESIZE_PATH.matcher(path);
        if ("PUT".equals(method) && resize.matches()) {
            int vmid = Integer.parseInt(resize.group(2));
            Map<String, String> form = form(exchange);
            String disk = form.get("disk");
            configs.get(vmid).computeIfPresent(disk, (k, v) -> v.replaceAll("size=[^,]+", "size=" + form.get("size")));
            sendJson(exchange, 200, Map.of("data", "")); // synchronous, like PVE 7
            return;
        }

        Matcher taskStatus = TASK_STATUS_PATH.matcher(path);
        if ("GET".equals(method) && taskStatus.matches()) {
            handleTaskStatus(exchange, taskStatus.group(2));
            return;
        }

        Matcher action = ACTION_PATH.matcher(path);
        if ("POST".equals(method) && action.matches()) {
            handleAction(exchange, action.group(1), Integer.parseInt(action.group(2)), action.group(3));
            return;
        }

        Matcher destroy = DESTROY_PATH.matcher(path);
        if ("DELETE".equals(method) && destroy.matches()) {
            handleAction(exchange, destroy.group(1), Integer.parseInt(destroy.group(2)), "destroy");
            return;
        }

        sendJson(exchange, 404, Map.of());
    }

    private void handleAction(HttpExchange exchange, String node, int vmid, String action) throws IOException {
        VmState vm = vms.get(vmid);
        if (vm == null || !vm.node.equals(node)) {
            sendJson(exchange, 404, Map.of());
            return;
        }
        String upid = "UPID:" + node + ":00000001:00000002:6612ABCD:qm" + action + ":" + vmid + ":test@pve!token:";
        tasks.put(upid, new TaskState(pollsBeforeCompletion, !nextTaskFails));
        nextTaskFails = false;
        sendJson(exchange, 200, Map.of("data", upid));
    }

    private void sendTask(HttpExchange exchange, String node, String type, int vmid) throws IOException {
        String upid = "UPID:" + node + ":00000001:00000002:6612ABCD:" + type + ":" + vmid + ":test@pve!token:" + tasks.size();
        tasks.put(upid, new TaskState(pollsBeforeCompletion, !nextTaskFails));
        nextTaskFails = false;
        sendJson(exchange, 200, Map.of("data", upid));
    }

    private static Map<String, String> form(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        Map<String, String> out = new LinkedHashMap<>();
        for (String pair : body.split("&")) {
            if (!pair.isEmpty()) {
                int eq = pair.indexOf('=');
                out.put(java.net.URLDecoder.decode(pair.substring(0, eq), java.nio.charset.StandardCharsets.UTF_8),
                        java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8));
            }
        }
        return out;
    }

    private void handleTaskStatus(HttpExchange exchange, String upid) throws IOException {
        TaskState task = tasks.get(upid);
        if (task == null) {
            sendJson(exchange, 404, Map.of());
            return;
        }
        if (task.pollsRemaining.getAndDecrement() > 0) {
            sendJson(exchange, 200, Map.of("data", Map.of("status", "running")));
            return;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "stopped");
        data.put("exitstatus", task.willSucceed ? "OK" : "task failed");
        sendJson(exchange, 200, Map.of("data", data));
    }

    private static Map<String, Object> toResourceJson(int vmid, VmState vm) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", "qemu");
        json.put("node", vm.node);
        json.put("vmid", vmid);
        json.put("name", vm.name);
        json.put("status", vm.status);
        json.put("maxcpu", vm.maxcpu);
        json.put("maxmem", vm.maxmemBytes);
        return json;
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private record VmState(String node, String name, String status, int maxcpu, long maxmemBytes) {}

    private static final class TaskState {
        final java.util.concurrent.atomic.AtomicInteger pollsRemaining;
        final boolean willSucceed;

        TaskState(int pollsRemaining, boolean willSucceed) {
            this.pollsRemaining = new java.util.concurrent.atomic.AtomicInteger(pollsRemaining);
            this.willSucceed = willSucceed;
        }
    }
}
