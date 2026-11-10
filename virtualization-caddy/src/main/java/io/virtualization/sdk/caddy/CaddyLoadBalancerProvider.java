package io.virtualization.sdk.caddy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.virtualization.sdk.caddy.client.CaddyApiClient;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.loadbalancer.Backend;
import io.virtualization.sdk.loadbalancer.HealthCheck;
import io.virtualization.sdk.loadbalancer.LoadBalancer;
import io.virtualization.sdk.loadbalancer.LoadBalancerProvider;
import io.virtualization.sdk.loadbalancer.Route;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link LoadBalancerProvider} backed by Caddy's admin API. Every mutating call does
 * read-current-config -&gt; modify the in-memory JSON tree -&gt; {@code POST /load} the whole
 * document — simpler and safer than Caddy's JSON-pointer partial-update endpoints, at the cost of
 * assuming sole ownership of the target Caddy instance's config (the same stance {@code
 * JsonFile*} stores take elsewhere in this SDK).
 *
 * <p>One {@link LoadBalancer} maps to one Caddy HTTP server ({@code apps.http.servers.<name>});
 * one {@link Route} maps to one host-matched route within it, whose {@code handle[0]} is always a
 * single {@code reverse_proxy} handler.
 */
public final class CaddyLoadBalancerProvider implements LoadBalancerProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final CaddyApiClient client;

    public CaddyLoadBalancerProvider(CaddyClientConfig config) {
        this(new CaddyApiClient(config));
    }

    /** Visible for tests, to inject a client talking to a fake server. */
    CaddyLoadBalancerProvider(CaddyApiClient client) {
        this.client = client;
    }

    @Override
    public List<LoadBalancer> listLoadBalancers() {
        ObjectNode servers = servers(readConfig());
        List<LoadBalancer> result = new ArrayList<>();
        servers.fieldNames().forEachRemaining(name -> result.add(toLoadBalancer(name, (ObjectNode) servers.get(name))));
        return result;
    }

    @Override
    public LoadBalancer getLoadBalancer(String name) {
        return toLoadBalancer(name, requireServer(readConfig(), name));
    }

    @Override
    public LoadBalancer createLoadBalancer(String name, List<String> listenAddresses) {
        ObjectNode root = readConfig();
        ObjectNode servers = servers(root);
        if (servers.has(name)) {
            throw new IllegalArgumentException("load balancer name '" + name + "' is already taken");
        }
        ObjectNode server = MAPPER.createObjectNode();
        ArrayNode listen = server.putArray("listen");
        listenAddresses.forEach(listen::add);
        server.putArray("routes");
        servers.set(name, server);
        client.loadConfig(root);
        return new LoadBalancer(name, listenAddresses);
    }

    @Override
    public void deleteLoadBalancer(String name) {
        ObjectNode root = readConfig();
        ObjectNode servers = servers(root);
        requireServerNode(servers, name);
        servers.remove(name);
        client.loadConfig(root);
    }

    @Override
    public List<Route> listRoutes(String loadBalancerName) {
        ObjectNode server = requireServer(readConfig(), loadBalancerName);
        List<Route> result = new ArrayList<>();
        for (JsonNode routeNode : routesArray(server)) {
            result.add(toRoute(loadBalancerName, (ObjectNode) routeNode));
        }
        return result;
    }

    @Override
    public Route getRoute(String loadBalancerName, String host) {
        ObjectNode server = requireServer(readConfig(), loadBalancerName);
        ArrayNode routes = routesArray(server);
        int index = findRouteIndex(routes, host);
        if (index < 0) {
            throw new ResourceNotFoundException("No route for host '" + host + "' on load balancer '" + loadBalancerName + "'");
        }
        return toRoute(loadBalancerName, (ObjectNode) routes.get(index));
    }

    @Override
    public Route createRoute(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck) {
        ObjectNode root = readConfig();
        ObjectNode server = requireServer(root, loadBalancerName);
        ArrayNode routes = routesArray(server);
        if (findRouteIndex(routes, host) >= 0) {
            throw new IllegalArgumentException("host '" + host + "' already has a route on load balancer '" + loadBalancerName + "'");
        }
        routes.add(buildRouteNode(host, backends, healthCheck));
        client.loadConfig(root);
        return new Route(loadBalancerName, host, backends, healthCheck);
    }

    @Override
    public Route updateRoute(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck) {
        ObjectNode root = readConfig();
        ObjectNode server = requireServer(root, loadBalancerName);
        ArrayNode routes = routesArray(server);
        int index = findRouteIndex(routes, host);
        if (index < 0) {
            throw new ResourceNotFoundException("No route for host '" + host + "' on load balancer '" + loadBalancerName + "'");
        }
        routes.set(index, buildRouteNode(host, backends, healthCheck));
        client.loadConfig(root);
        return new Route(loadBalancerName, host, backends, healthCheck);
    }

    @Override
    public void deleteRoute(String loadBalancerName, String host) {
        ObjectNode root = readConfig();
        ObjectNode server = requireServer(root, loadBalancerName);
        ArrayNode routes = routesArray(server);
        int index = findRouteIndex(routes, host);
        if (index < 0) {
            throw new ResourceNotFoundException("No route for host '" + host + "' on load balancer '" + loadBalancerName + "'");
        }
        routes.remove(index);
        client.loadConfig(root);
    }

    private ObjectNode readConfig() {
        JsonNode config = client.getConfig();
        return config != null && config.isObject() ? (ObjectNode) config : MAPPER.createObjectNode();
    }

    private static ObjectNode servers(ObjectNode root) {
        return objectChild(objectChild(objectChild(root, "apps"), "http"), "servers");
    }

    private static ObjectNode objectChild(ObjectNode parent, String field) {
        JsonNode existing = parent.get(field);
        if (existing != null && existing.isObject()) {
            return (ObjectNode) existing;
        }
        ObjectNode created = MAPPER.createObjectNode();
        parent.set(field, created);
        return created;
    }

    private static ObjectNode requireServer(ObjectNode root, String loadBalancerName) {
        return requireServerNode(servers(root), loadBalancerName);
    }

    private static ObjectNode requireServerNode(ObjectNode servers, String loadBalancerName) {
        JsonNode server = servers.get(loadBalancerName);
        if (server == null || !server.isObject()) {
            throw new ResourceNotFoundException("No load balancer named '" + loadBalancerName + "'");
        }
        return (ObjectNode) server;
    }

    private static ArrayNode routesArray(ObjectNode server) {
        JsonNode existing = server.get("routes");
        if (existing != null && existing.isArray()) {
            return (ArrayNode) existing;
        }
        ArrayNode created = MAPPER.createArrayNode();
        server.set("routes", created);
        return created;
    }

    private static int findRouteIndex(ArrayNode routes, String host) {
        for (int i = 0; i < routes.size(); i++) {
            if (routeHost(routes.get(i)).equals(host)) {
                return i;
            }
        }
        return -1;
    }

    private static String routeHost(JsonNode routeNode) {
        return routeNode.path("match").path(0).path("host").path(0).asText();
    }

    private static LoadBalancer toLoadBalancer(String name, ObjectNode server) {
        List<String> listen = new ArrayList<>();
        server.path("listen").forEach(node -> listen.add(node.asText()));
        return new LoadBalancer(name, listen);
    }

    private static Route toRoute(String loadBalancerName, ObjectNode routeNode) {
        String host = routeHost(routeNode);
        JsonNode handler = routeNode.path("handle").path(0);
        List<Backend> backends = new ArrayList<>();
        handler.path("upstreams").forEach(upstream -> backends.add(parseBackend(upstream.path("dial").asText())));
        HealthCheck healthCheck = null;
        JsonNode active = handler.path("health_checks").path("active");
        if (!active.isMissingNode()) {
            healthCheck = new HealthCheck(
                    active.path("path").asText(), parseDuration(active.path("interval").asText()), parseDuration(active.path("timeout").asText()));
        }
        return new Route(loadBalancerName, host, backends, healthCheck);
    }

    private static Backend parseBackend(String dial) {
        int colon = dial.lastIndexOf(':');
        return new Backend(dial.substring(0, colon), Integer.parseInt(dial.substring(colon + 1)));
    }

    /** The smallest correct subset for a homelab health check — whole seconds only, not a general duration parser. */
    private static Duration parseDuration(String text) {
        if (text.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(text.substring(0, text.length() - 1)));
        }
        throw new IllegalStateException("Unsupported Caddy duration format '" + text + "'");
    }

    private static String formatDuration(Duration duration) {
        return duration.toSeconds() + "s";
    }

    private static ObjectNode buildRouteNode(String host, List<Backend> backends, HealthCheck healthCheck) {
        ObjectNode route = MAPPER.createObjectNode();
        ArrayNode match = route.putArray("match");
        ObjectNode matchEntry = MAPPER.createObjectNode();
        matchEntry.putArray("host").add(host);
        match.add(matchEntry);

        ObjectNode handler = MAPPER.createObjectNode();
        handler.put("handler", "reverse_proxy");
        ArrayNode upstreams = handler.putArray("upstreams");
        for (Backend backend : backends) {
            upstreams.addObject().put("dial", backend.host() + ":" + backend.port());
        }
        if (healthCheck != null) {
            ObjectNode active = handler.putObject("health_checks").putObject("active");
            active.put("path", healthCheck.path());
            active.put("interval", formatDuration(healthCheck.interval()));
            active.put("timeout", formatDuration(healthCheck.timeout()));
        }
        route.putArray("handle").add(handler);
        return route;
    }
}
