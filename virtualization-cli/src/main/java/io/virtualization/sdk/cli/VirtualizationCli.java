package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.config.ConfigLoader;
import io.virtualization.sdk.cli.output.OutputFormat;
import io.virtualization.sdk.core.VirtualizationClient;
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
            DomainCommand.class, DnsCommand.class, CertificateCommand.class
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

    @Spec
    CommandSpec spec;

    private final Function<Path, VirtualizationClient> clientLoader;
    private VirtualizationClient cachedClient;

    public VirtualizationCli() {
        this(ConfigLoader::loadClient);
    }

    /** Visible for tests, to inject a client without reading real configuration or hitting a network. */
    VirtualizationCli(Function<Path, VirtualizationClient> clientLoader) {
        this.clientLoader = clientLoader;
    }

    VirtualizationClient client() {
        if (cachedClient == null) {
            cachedClient = clientLoader.apply(resolveConfigPath());
        }
        return cachedClient;
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

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
