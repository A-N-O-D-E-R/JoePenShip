package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.network.NetworkId;
import io.virtualization.sdk.network.Subnet;
import io.virtualization.sdk.network.SubnetId;
import io.virtualization.sdk.network.SubnetManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/networks/{networkId}/subnets} — over the always-registered {@link SubnetManager} bean. */
@RestController
@RequestMapping("/api/v1/networks/{networkId}/subnets")
public class SubnetController {

    private final SubnetManager subnetManager;

    public SubnetController(SubnetManager subnetManager) {
        this.subnetManager = subnetManager;
    }

    @GetMapping
    public List<Subnet> list(@PathVariable String networkId) {
        return subnetManager.list(new NetworkId(networkId));
    }

    @PostMapping
    public Subnet create(@PathVariable String networkId, @RequestBody CreateSubnetRequestBody request) {
        Objects.requireNonNull(request.cidr(), "'cidr' is required");
        return subnetManager.create(new NetworkId(networkId), request.cidr(), request.gateway());
    }

    @GetMapping("/{id}")
    public Subnet get(@PathVariable String networkId, @PathVariable String id) {
        return subnetManager.get(new SubnetId(id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String networkId, @PathVariable String id) {
        subnetManager.delete(new SubnetId(id));
    }
}
