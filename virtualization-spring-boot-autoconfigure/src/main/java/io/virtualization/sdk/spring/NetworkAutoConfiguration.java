package io.virtualization.sdk.spring;

import io.virtualization.sdk.network.DefaultFirewallRuleManager;
import io.virtualization.sdk.network.DefaultIpamService;
import io.virtualization.sdk.network.DefaultNetworkManager;
import io.virtualization.sdk.network.DefaultSubnetManager;
import io.virtualization.sdk.network.FirewallRuleManager;
import io.virtualization.sdk.network.FirewallRuleRepository;
import io.virtualization.sdk.network.IpAllocationRepository;
import io.virtualization.sdk.network.IpamService;
import io.virtualization.sdk.network.NetworkManager;
import io.virtualization.sdk.network.NetworkRepository;
import io.virtualization.sdk.network.SubnetManager;
import io.virtualization.sdk.network.SubnetRepository;
import io.virtualization.sdk.network.store.JsonFileNetworkStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.nio.file.Path;

/**
 * Wires a {@link JsonFileNetworkStore} into the four {@code virtualization-network} repositories
 * and their managers — always created, no {@code virtualization.network.enabled} flag (same
 * "always-on" stance {@link ProjectAutoConfiguration} takes: a network needs no upstream provider
 * configuration to make sense). Unlike {@link ProjectAutoConfiguration}, the default store is
 * **durable**, not in-memory — losing IP allocation state on restart risks handing out an address
 * already live on a running VPS, a real conflict rather than merely inconvenient state loss (the
 * same reasoning {@code IamAutoConfiguration} applies to IAM state).
 */
@AutoConfiguration
@EnableConfigurationProperties(NetworkProperties.class)
public class NetworkAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JsonFileNetworkStore networkStore(NetworkProperties properties) {
        Path stateFile = properties.stateFile() != null
                ? Path.of(properties.stateFile())
                : Path.of(System.getProperty("user.home"), ".virtualization", "network.json");
        return new JsonFileNetworkStore(stateFile);
    }

    @Bean
    @ConditionalOnMissingBean
    public NetworkRepository networkRepository(JsonFileNetworkStore store) {
        return store.networks();
    }

    @Bean
    @ConditionalOnMissingBean
    public SubnetRepository subnetRepository(JsonFileNetworkStore store) {
        return store.subnets();
    }

    @Bean
    @ConditionalOnMissingBean
    public IpAllocationRepository ipAllocationRepository(JsonFileNetworkStore store) {
        return store.ipAllocations();
    }

    @Bean
    @ConditionalOnMissingBean
    public FirewallRuleRepository firewallRuleRepository(JsonFileNetworkStore store) {
        return store.firewallRules();
    }

    @Bean
    @ConditionalOnMissingBean
    public NetworkManager networkManager(NetworkRepository repository) {
        return new DefaultNetworkManager(repository);
    }

    @Bean
    @ConditionalOnMissingBean
    public SubnetManager subnetManager(SubnetRepository repository, NetworkRepository networkRepository) {
        return new DefaultSubnetManager(repository, networkRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public IpamService ipamService(SubnetRepository subnetRepository, IpAllocationRepository allocationRepository) {
        return new DefaultIpamService(subnetRepository, allocationRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public FirewallRuleManager firewallRuleManager(FirewallRuleRepository repository, NetworkRepository networkRepository) {
        return new DefaultFirewallRuleManager(repository, networkRepository);
    }
}
