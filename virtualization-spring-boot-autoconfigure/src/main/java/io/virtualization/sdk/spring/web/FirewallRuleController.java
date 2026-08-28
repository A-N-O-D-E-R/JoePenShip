package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.network.FirewallRule;
import io.virtualization.sdk.network.FirewallRuleId;
import io.virtualization.sdk.network.FirewallRuleManager;
import io.virtualization.sdk.network.NetworkId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/networks/{networkId}/firewall-rules} — over the always-registered {@link FirewallRuleManager} bean. Modeled only; no provider enforces these rules yet. */
@RestController
@RequestMapping("/api/v1/networks/{networkId}/firewall-rules")
public class FirewallRuleController {

    private final FirewallRuleManager firewallRuleManager;

    public FirewallRuleController(FirewallRuleManager firewallRuleManager) {
        this.firewallRuleManager = firewallRuleManager;
    }

    @GetMapping
    public List<FirewallRule> list(@PathVariable String networkId) {
        return firewallRuleManager.list(new NetworkId(networkId));
    }

    @PostMapping
    public FirewallRule create(@PathVariable String networkId, @RequestBody CreateFirewallRuleRequestBody request) {
        Objects.requireNonNull(request.direction(), "'direction' is required");
        Objects.requireNonNull(request.protocol(), "'protocol' is required");
        Objects.requireNonNull(request.cidr(), "'cidr' is required");
        Objects.requireNonNull(request.action(), "'action' is required");
        return firewallRuleManager.create(new NetworkId(networkId), request.direction(), request.protocol(),
                request.portRangeStart(), request.portRangeEnd(), request.cidr(), request.action(), request.priority());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String networkId, @PathVariable String id) {
        firewallRuleManager.delete(new FirewallRuleId(id));
    }
}
