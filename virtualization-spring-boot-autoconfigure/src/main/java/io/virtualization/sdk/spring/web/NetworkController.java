package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.NetworkId;
import io.virtualization.sdk.network.NetworkManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/networks} — over the always-registered {@link NetworkManager} bean (see {@link io.virtualization.sdk.spring.NetworkAutoConfiguration}). */
@RestController
@RequestMapping("/api/v1/networks")
public class NetworkController {

    private final NetworkManager networkManager;

    public NetworkController(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @GetMapping
    public List<Network> list() {
        return networkManager.list();
    }

    @PostMapping
    public Network create(@RequestBody CreateNetworkRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        return networkManager.create(request.name(), request.description());
    }

    @GetMapping("/{id}")
    public Network get(@PathVariable String id) {
        return networkManager.get(new NetworkId(id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        networkManager.delete(new NetworkId(id));
    }
}
