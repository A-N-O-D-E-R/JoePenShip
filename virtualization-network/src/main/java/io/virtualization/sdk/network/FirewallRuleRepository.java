package io.virtualization.sdk.network;

import java.util.List;
import java.util.Optional;

public interface FirewallRuleRepository {
    void save(FirewallRule rule);
    Optional<FirewallRule> findById(FirewallRuleId id);
    List<FirewallRule> findByNetwork(NetworkId networkId);
    List<FirewallRule> findAll();
    void delete(FirewallRuleId id);
}
