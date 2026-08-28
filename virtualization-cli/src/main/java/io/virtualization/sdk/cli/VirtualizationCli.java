package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.config.ConfigLoader;
import io.virtualization.sdk.cli.config.LoadBalancerConfigLoader;
import io.virtualization.sdk.cli.config.StorageConfigLoader;
import io.virtualization.sdk.cli.output.OutputFormat;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.loadbalancer.LoadBalancerProviderRegistry;
import io.virtualization.sdk.storage.StorageProviderRegistry;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Root {@code virtualization} command. Options declared here with {@code scope = INHERIT} are
 * available to every subcommand, at any position in the argument list — before or after the
 * subcommand name — e.g. both {@code virtualization --provider production vm list} and
 * {@code virtualization vm list --provider production} work.
 */
@Command(
        name = "virtualization",
        mixinStandardHelpOptions = true,
        versionProvider = ManifestVersionProvider.class,
        subcommands = {
            ProviderCommand.class, VmCommand.class, ImageCommand.class, WorkloadCommand.class, VpsCommand.class,
            DomainCommand.class, DnsCommand.class, CertificateCommand.class, IamCommand.class, ProjectCommand.class,
            NetworkCommand.class, SubnetCommand.class, IpCommand.class, FirewallRuleCommand.class,
            StoragePoolCommand.class, VolumeCommand.class, LoadBalancerCommand.class, RouteCommand.class
        },
        description = "Manage virtual machines and images across Proxmox, Incus and QEMU providers.")
public final class VirtualizationCli implements Callable<Integer> {

    @Option(names = "--provider", scope = ScopeType.INHERIT, description = "Name of the configured provider to use.")
    String provider;

    @Option(
            names = "--output",
            scope = ScopeType.INHERIT,
            defaultValue = "table",
            description = "Output format: ${COMPLETION-CANDIDATES} (default: table).")
    OutputFormat output;

    @Option(
            names = "--config",
            scope = ScopeType.INHERIT,
            description = "Path to the provider configuration YAML file (default: ~/.virtualization/config.yaml).")
    Path configPath;

    @Option(
            names = "--api-token",
            scope = ScopeType.INHERIT,
            description = "API token to authenticate with when virtualization.iam.enabled is true (prefer the "
                    + "VIRTUALIZATION_API_TOKEN environment variable — a flag value may land in shell history).")
    String apiToken;

    @Spec
    CommandSpec spec;

    private final Function<Path, VirtualizationClient> clientLoader;
    private final Function<Path, StorageProviderRegistry> storageProviderRegistryLoader;
    private final Function<Path, LoadBalancerProviderRegistry> loadBalancerProviderRegistryLoader;
    private VirtualizationClient cachedClient;
    private StorageProviderRegistry cachedStorageProviderRegistry;
    private LoadBalancerProviderRegistry cachedLoadBalancerProviderRegistry;

    public VirtualizationCli() {
        this(ConfigLoader::loadClient, StorageConfigLoader::loadRegistry, LoadBalancerConfigLoader::loadRegistry);
    }

    /** Visible for tests, to inject a client without reading real configuration or hitting a network. */
    VirtualizationCli(Function<Path, VirtualizationClient> clientLoader) {
        this(clientLoader, StorageConfigLoader::loadRegistry, LoadBalancerConfigLoader::loadRegistry);
    }

    /** Visible for tests, to also inject a {@link StorageProviderRegistry} without reading real configuration. */
    VirtualizationCli(Function<Path, VirtualizationClient> clientLoader, Function<Path, StorageProviderRegistry> storageProviderRegistryLoader) {
        this(clientLoader, storageProviderRegistryLoader, LoadBalancerConfigLoader::loadRegistry);
    }

    /** Visible for tests, to also inject a {@link LoadBalancerProviderRegistry} without reading real configuration. */
    VirtualizationCli(
            Function<Path, VirtualizationClient> clientLoader, Function<Path, StorageProviderRegistry> storageProviderRegistryLoader,
            Function<Path, LoadBalancerProviderRegistry> loadBalancerProviderRegistryLoader) {
        this.clientLoader = clientLoader;
        this.storageProviderRegistryLoader = storageProviderRegistryLoader;
        this.loadBalancerProviderRegistryLoader = loadBalancerProviderRegistryLoader;
    }

    VirtualizationClient client() {
        if (cachedClient == null) {
            cachedClient = clientLoader.apply(resolveConfigPath());
        }
        return cachedClient;
    }

    StorageProviderRegistry storageProviderRegistry() {
        if (cachedStorageProviderRegistry == null) {
            cachedStorageProviderRegistry = storageProviderRegistryLoader.apply(resolveConfigPath());
        }
        return cachedStorageProviderRegistry;
    }

    LoadBalancerProviderRegistry loadBalancerProviderRegistry() {
        if (cachedLoadBalancerProviderRegistry == null) {
            cachedLoadBalancerProviderRegistry = loadBalancerProviderRegistryLoader.apply(resolveConfigPath());
        }
        return cachedLoadBalancerProviderRegistry;
    }

    Path resolveConfigPath() {
        return configPath != null
                ? configPath
                : Path.of(System.getProperty("user.home"), ".virtualization", "config.yaml");
    }

    /** VPS state lives alongside the provider config file — same directory, {@code vps.json}. */
    Path vpsStatePath() {
        return resolveConfigPath().resolveSibling("vps.json");
    }

    /** DNS record state lives alongside the provider config file, one file per configured provider name. */
    Path dnsStatePath(String providerName) {
        return resolveConfigPath().resolveSibling("dns-" + providerName + ".json");
    }

    /** Certificate metadata lives alongside the provider config file — same directory, {@code certificates.json}. */
    Path certificateStatePath() {
        return resolveConfigPath().resolveSibling("certificates.json");
    }

    /** AES/GCM-encrypted certificate material lives alongside the provider config file — same directory, {@code certificates.enc}. */
    Path certificateMaterialPath() {
        return resolveConfigPath().resolveSibling("certificates.enc");
    }

    /** IAM state (users/roles/groups/API credentials) lives alongside the provider config file — same directory, {@code iam.json}. */
    Path iamStatePath() {
        return resolveConfigPath().resolveSibling("iam.json");
    }

    /** Project metadata lives alongside the provider config file — same directory, {@code projects.json}. */
    Path projectStatePath() {
        return resolveConfigPath().resolveSibling("projects.json");
    }

    /** Network/subnet/IP-allocation/firewall-rule state lives alongside the provider config file — same directory, {@code network.json}. */
    Path networkStatePath() {
        return resolveConfigPath().resolveSibling("network.json");
    }

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
