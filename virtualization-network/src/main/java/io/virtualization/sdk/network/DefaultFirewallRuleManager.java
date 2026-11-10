package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** The only {@link FirewallRuleManager} implementation. */
public final class DefaultFirewallRuleManager implements FirewallRuleManager {

    private final FirewallRuleRepository repository;
    private final NetworkRepository networkRepository;

    public DefaultFirewallRuleManager(FirewallRuleRepository repository, NetworkRepository networkRepository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.networkRepository = Objects.requireNonNull(networkRepository, "networkRepository must not be null");
    }

    @Override
    public FirewallRule create(NetworkId networkId, FirewallDirection direction, FirewallProtocol protocol,
                                Integer portRangeStart, Integer portRangeEnd, String cidr, FirewallAction action, int priority) {
        Objects.requireNonNull(networkId, "networkId must not be null");
        if (networkRepository.findById(networkId).isEmpty()) {
            throw new ResourceNotFoundException("No network with id '" + networkId.value() + "'");
        }
        FirewallRule rule = new FirewallRule(FirewallRuleId.generate(), networkId, direction, protocol,
                portRangeStart, portRangeEnd, cidr, action, priority, Instant.now());
        repository.save(rule);
        return rule;
    }

    @Override
    public FirewallRule get(FirewallRuleId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No firewall rule with id '" + id.value() + "'"));
    }

    @Override
    public List<FirewallRule> list(NetworkId networkId) {
        Objects.requireNonNull(networkId, "networkId must not be null");
        return repository.findByNetwork(networkId);
    }

    @Override
    public void delete(FirewallRuleId id) {
        get(id);
        repository.delete(id);
    }
}
