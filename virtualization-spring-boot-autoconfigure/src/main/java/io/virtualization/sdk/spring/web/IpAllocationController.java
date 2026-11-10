package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.network.IpAllocation;
import io.virtualization.sdk.network.IpAllocationId;
import io.virtualization.sdk.network.IpamService;
import io.virtualization.sdk.network.SubnetId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/networks/{networkId}/subnets/{subnetId}/ips} — over the always-registered {@link IpamService} bean. */
@RestController
@RequestMapping("/api/v1/networks/{networkId}/subnets/{subnetId}/ips")
public class IpAllocationController {

    private final IpamService ipamService;

    public IpAllocationController(IpamService ipamService) {
        this.ipamService = ipamService;
    }

    @GetMapping
    public List<IpAllocation> list(@PathVariable String networkId, @PathVariable String subnetId) {
        return ipamService.list(new SubnetId(subnetId));
    }

    @PostMapping
    public IpAllocation allocate(@PathVariable String networkId, @PathVariable String subnetId, @RequestBody AllocateIpRequestBody request) {
        Objects.requireNonNull(request.ownerRef(), "'ownerRef' is required");
        return ipamService.allocate(new SubnetId(subnetId), request.ownerRef());
    }

    @DeleteMapping("/{id}")
    public void release(@PathVariable String networkId, @PathVariable String subnetId, @PathVariable String id) {
        ipamService.release(new IpAllocationId(id));
    }
}
