package io.virtualization.sdk.network;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryFirewallRuleRepository implements FirewallRuleRepository {

    private final ConcurrentHashMap<FirewallRuleId, FirewallRule> store = new ConcurrentHashMap<>();

    @Override
    public void save(FirewallRule rule) {
        store.put(rule.id(), rule);
    }

    @Override
    public Optional<FirewallRule> findById(FirewallRuleId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<FirewallRule> findByNetwork(NetworkId networkId) {
        return store.values().stream().filter(r -> r.networkId().equals(networkId)).toList();
    }

    @Override
    public List<FirewallRule> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(FirewallRuleId id) {
        store.remove(id);
    }
}
