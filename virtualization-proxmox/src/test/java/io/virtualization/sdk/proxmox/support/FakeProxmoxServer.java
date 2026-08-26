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
    private static final Pattern TASK_STATUS_PATH = Pattern.compile("/api2/json/nodes/([^/]+)/tasks/(.+)/status");

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpServer server;
    private final String expectedAuthorizationHeader;
    private final Map<Integer, VmState> vms = new ConcurrentHashMap<>();
    private final Map<String, TaskState> tasks = new ConcurrentHashMap<>();
    private volatile int pollsBeforeCompletion = 0;
    private volatile boolean nextTaskFails = false;

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
            vms.forEach((vmid, vm) -> resources.add(toResourceJson(vmid, vm)));
            sendJson(exchange, 200, Map.of("data", resources));
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
