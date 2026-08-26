package io.virtualization.sdk.cli;

import io.virtualization.sdk.certificate.CertificateManager;
import io.virtualization.sdk.certificate.CertificateStore;
import io.virtualization.sdk.certificate.DefaultCertificateManager;
import io.virtualization.sdk.cli.certificate.EncryptedFileCertificateStore;
import io.virtualization.sdk.cli.certificate.JsonFileCertificateRepository;
import io.virtualization.sdk.cli.certificate.Passphrases;
import io.virtualization.sdk.cli.config.CertificateConfigLoader;
import io.virtualization.sdk.cli.config.DnsConfigLoader;
import io.virtualization.sdk.cli.output.OutputWriter;
import io.virtualization.sdk.cli.vps.JsonFileVpsRepository;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.domain.DefaultDomainManager;
import io.virtualization.sdk.domain.DomainManager;
import io.virtualization.sdk.domain.InMemoryDomainRepository;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import io.virtualization.sdk.vps.DefaultVpsManager;
import io.virtualization.sdk.vps.DefaultVpsProvisioner;
import io.virtualization.sdk.vps.VpsManager;
import io.virtualization.sdk.vps.VpsProvisioner;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.io.PrintWriter;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

/** Shared plumbing for leaf CLI commands: reach the root command's options and client, regardless of nesting depth. */
abstract class AbstractCliCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    protected final VirtualizationCli root() {
        return (VirtualizationCli) spec.root().userObject();
    }

    protected final VirtualizationClient client() {
        return root().client();
    }

    protected final String requireProviderName() {
        String name = root().provider;
        if (name == null || name.isBlank()) {
            throw new ParameterException(spec.commandLine(), "Missing required option: '--provider=<provider>'");
        }
        return name;
    }

    protected final VirtualizationProvider provider() {
        return client().provider(requireProviderName());
    }

    protected final ImageProvider images() {
        return client().images(requireProviderName());
    }

    /**
     * A fresh {@link VpsManager} per invocation, backed by a {@link JsonFileVpsRepository} — the
     * CLI process doesn't stay alive between commands, so state has to live on disk, not in memory.
     */
    protected final VpsManager vpsManager() {
        String providerName = requireProviderName();
        VpsProvisioner provisioner = new DefaultVpsProvisioner(client().provider(providerName), client().images(providerName));
        return new DefaultVpsManager(new JsonFileVpsRepository(root().vpsStatePath()), provisioner);
    }

    /** A fresh {@link DnsProviderRegistry} per invocation, built from {@code virtualization.dns.providers.*} in the config file. */
    protected final DnsProviderRegistry dnsProviderRegistry() {
        return DnsConfigLoader.loadRegistry(root().resolveConfigPath(), root()::dnsStatePath);
    }

    protected final DomainManager domainManager() {
        return new DefaultDomainManager(new InMemoryDomainRepository(), dnsProviderRegistry());
    }

    protected final CertificateStore certificateStore(Supplier<char[]> passphrase) {
        return new EncryptedFileCertificateStore(root().certificateMaterialPath(), passphrase);
    }

    /** A fresh {@link CertificateManager} per invocation, backed by a {@link JsonFileCertificateRepository}. */
    protected final CertificateManager certificateManager(Supplier<char[]> passphrase) {
        CertificateStore store = certificateStore(passphrase);
        return new DefaultCertificateManager(
                new JsonFileCertificateRepository(root().certificateStatePath()),
                CertificateConfigLoader.loadRegistry(root().resolveConfigPath(), dnsProviderRegistry(), store));
    }

    /** For commands that never touch {@link CertificateStore} ({@code list}/{@code get}/{@code renew}/{@code revoke}) — no passphrase required. */
    protected final CertificateManager certificateManager() {
        return certificateManager(Passphrases.unavailable());
    }

    protected final OutputWriter outputWriter() {
        return OutputWriter.forFormat(root().output);
    }

    protected final PrintWriter out() {
        return spec.commandLine().getOut();
    }
}
