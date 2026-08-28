package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.network.DefaultFirewallRuleManager;
import io.virtualization.sdk.network.DefaultNetworkManager;
import io.virtualization.sdk.network.FirewallAction;
import io.virtualization.sdk.network.FirewallDirection;
import io.virtualization.sdk.network.FirewallProtocol;
import io.virtualization.sdk.network.FirewallRule;
import io.virtualization.sdk.network.FirewallRuleManager;
import io.virtualization.sdk.network.InMemoryFirewallRuleRepository;
import io.virtualization.sdk.network.InMemoryNetworkRepository;
import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.NetworkManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FirewallRuleControllerTest {

    private FirewallRuleController controller;
    private Network network;

    @BeforeEach
    void setUp() {
        InMemoryNetworkRepository networkRepository = new InMemoryNetworkRepository();
        NetworkManager networkManager = new DefaultNetworkManager(networkRepository);
        FirewallRuleManager ruleManager = new DefaultFirewallRuleManager(new InMemoryFirewallRuleRepository(), networkRepository);
        controller = new FirewallRuleController(ruleManager);
        network = networkManager.create("public", null);
    }

    @Test
    void createReturnsTheCreatedRule() {
        FirewallRule created = controller.create(network.id().value(),
                new CreateFirewallRuleRequestBody(FirewallDirection.INBOUND, FirewallProtocol.TCP, 80, 443, "0.0.0.0/0", FirewallAction.ALLOW, 100));

        assertThat(created.direction()).isEqualTo(FirewallDirection.INBOUND);
        assertThat(created.action()).isEqualTo(FirewallAction.ALLOW);
    }

    @Test
    void listReturnsEveryRuleInTheNetwork() {
        FirewallRule created = controller.create(network.id().value(),
                new CreateFirewallRuleRequestBody(FirewallDirection.OUTBOUND, FirewallProtocol.ANY, null, null, "0.0.0.0/0", FirewallAction.DENY, 0));

        assertThat(controller.list(network.id().value())).containsExactly(created);
    }

    @Test
    void deleteRemovesTheRule() {
        FirewallRule created = controller.create(network.id().value(),
                new CreateFirewallRuleRequestBody(FirewallDirection.INBOUND, FirewallProtocol.ANY, null, null, "0.0.0.0/0", FirewallAction.DENY, 0));

        controller.delete(network.id().value(), created.id().value());

        assertThat(controller.list(network.id().value())).isEmpty();
    }
}
