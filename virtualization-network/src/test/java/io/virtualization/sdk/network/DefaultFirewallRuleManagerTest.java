package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFirewallRuleManagerTest {

    private DefaultNetworkManager networkManager;
    private DefaultFirewallRuleManager ruleManager;

    @BeforeEach
    void setUp() {
        InMemoryNetworkRepository networkRepository = new InMemoryNetworkRepository();
        networkManager = new DefaultNetworkManager(networkRepository);
        ruleManager = new DefaultFirewallRuleManager(new InMemoryFirewallRuleRepository(), networkRepository);
    }

    @Test
    void createThenGetRoundTrips() {
        Network network = networkManager.create("public", null);

        FirewallRule created = ruleManager.create(network.id(), FirewallDirection.INBOUND, FirewallProtocol.TCP,
                80, 443, "0.0.0.0/0", FirewallAction.ALLOW, 100);

        assertThat(ruleManager.get(created.id())).isEqualTo(created);
    }

    @Test
    void createRejectsAnUnknownNetwork() {
        assertThatThrownBy(() -> ruleManager.create(NetworkId.generate(), FirewallDirection.INBOUND, FirewallProtocol.ANY,
                null, null, "0.0.0.0/0", FirewallAction.DENY, 0))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRejectsAPortRangeStartAfterEnd() {
        Network network = networkManager.create("public", null);

        assertThatThrownBy(() -> ruleManager.create(network.id(), FirewallDirection.INBOUND, FirewallProtocol.TCP,
                443, 80, "0.0.0.0/0", FirewallAction.ALLOW, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsOnlyOneSideOfThePortRangeSet() {
        Network network = networkManager.create("public", null);

        assertThatThrownBy(() -> ruleManager.create(network.id(), FirewallDirection.INBOUND, FirewallProtocol.TCP,
                80, null, "0.0.0.0/0", FirewallAction.ALLOW, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listReturnsOnlyRulesForTheGivenNetwork() {
        Network first = networkManager.create("public", null);
        Network second = networkManager.create("private", null);
        FirewallRule inFirst = ruleManager.create(first.id(), FirewallDirection.OUTBOUND, FirewallProtocol.ANY,
                null, null, "0.0.0.0/0", FirewallAction.ALLOW, 0);
        ruleManager.create(second.id(), FirewallDirection.OUTBOUND, FirewallProtocol.ANY,
                null, null, "0.0.0.0/0", FirewallAction.ALLOW, 0);

        assertThat(ruleManager.list(first.id())).containsExactly(inFirst);
    }

    @Test
    void deleteRemovesTheRule() {
        Network network = networkManager.create("public", null);
        FirewallRule created = ruleManager.create(network.id(), FirewallDirection.INBOUND, FirewallProtocol.ANY,
                null, null, "0.0.0.0/0", FirewallAction.DENY, 0);

        ruleManager.delete(created.id());

        assertThatThrownBy(() -> ruleManager.get(created.id())).isInstanceOf(ResourceNotFoundException.class);
    }
}
