package io.virtualization.sdk.cli;

import io.virtualization.sdk.certificate.CertificateManager;
import io.virtualization.sdk.certificate.CertificateStore;
import io.virtualization.sdk.certificate.DefaultCertificateManager;
import io.virtualization.sdk.cli.certificate.EncryptedFileCertificateStore;
import io.virtualization.sdk.cli.certificate.JsonFileCertificateRepository;
import io.virtualization.sdk.cli.certificate.Passphrases;
import io.virtualization.sdk.cli.config.CertificateConfigLoader;
import io.virtualization.sdk.cli.config.DnsConfigLoader;
import io.virtualization.sdk.cli.config.IamConfigLoader;
import io.virtualization.sdk.cli.iam.ApiCredentials;
import io.virtualization.sdk.cli.output.OutputWriter;
import io.virtualization.sdk.cli.vps.JsonFileVpsRepository;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.AuthorizationException;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.domain.DefaultDomainManager;
import io.virtualization.sdk.domain.DomainManager;
import io.virtualization.sdk.domain.InMemoryDomainRepository;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.AuthorizationService;
import io.virtualization.sdk.iam.Bootstrap;
import io.virtualization.sdk.iam.DefaultApiCredentialManager;
import io.virtualization.sdk.iam.DefaultAuthorizationService;
import io.virtualization.sdk.iam.DefaultGroupManager;
import io.virtualization.sdk.iam.DefaultRoleManager;
import io.virtualization.sdk.iam.DefaultUserManager;
import io.virtualization.sdk.iam.GroupManager;
import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.RoleManager;
import io.virtualization.sdk.iam.User;
import io.virtualization.sdk.iam.UserManager;
import io.virtualization.sdk.iam.store.JsonFileIamStore;
import io.virtualization.sdk.cli.project.JsonFileProjectRepository;
import io.virtualization.sdk.network.DefaultFirewallRuleManager;
import io.virtualization.sdk.network.DefaultIpamService;
import io.virtualization.sdk.network.DefaultNetworkManager;
import io.virtualization.sdk.network.DefaultSubnetManager;
import io.virtualization.sdk.network.FirewallRuleManager;
import io.virtualization.sdk.network.IpamService;
import io.virtualization.sdk.network.NetworkManager;
import io.virtualization.sdk.network.SubnetManager;
import io.virtualization.sdk.network.store.JsonFileNetworkStore;
import io.virtualization.sdk.project.DefaultProjectManager;
import io.virtualization.sdk.project.ProjectManager;
import io.virtualization.sdk.loadbalancer.LoadBalancerProvider;
import io.virtualization.sdk.storage.StorageProvider;
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

    protected final PrintWriter err() {
        return spec.commandLine().getErr();
    }

    private JsonFileIamStore iamStore() {
        return new JsonFileIamStore(root().iamStatePath());
    }

    protected final AuthorizationService authorizationService() {
        JsonFileIamStore store = iamStore();
        return new DefaultAuthorizationService(store.roles(), store.groups());
    }

    protected final ApiCredentialManager apiCredentialManager() {
        JsonFileIamStore store = iamStore();
        return new DefaultApiCredentialManager(store.apiCredentials(), store.users());
    }

    protected final UserManager userManager() {
        return new DefaultUserManager(iamStore().users());
    }

    protected final RoleManager roleManager() {
        return new DefaultRoleManager(iamStore().roles());
    }

    protected final GroupManager groupManager() {
        JsonFileIamStore store = iamStore();
        return new DefaultGroupManager(store.groups(), store.users());
    }

    /** A fresh {@link ProjectManager} per invocation, backed by a {@link JsonFileProjectRepository}. */
    protected final ProjectManager projectManager() {
        return new DefaultProjectManager(new JsonFileProjectRepository(root().projectStatePath()));
    }

    /** A fresh {@link JsonFileNetworkStore} per invocation, backed by {@code root().networkStatePath()}. */
    protected final JsonFileNetworkStore networkStore() {
        return new JsonFileNetworkStore(root().networkStatePath());
    }

    protected final NetworkManager networkManager() {
        return new DefaultNetworkManager(networkStore().networks());
    }

    protected final SubnetManager subnetManager() {
        JsonFileNetworkStore store = networkStore();
        return new DefaultSubnetManager(store.subnets(), store.networks());
    }

    protected final IpamService ipamService() {
        JsonFileNetworkStore store = networkStore();
        return new DefaultIpamService(store.subnets(), store.ipAllocations());
    }

    protected final FirewallRuleManager firewallRuleManager() {
        JsonFileNetworkStore store = networkStore();
        return new DefaultFirewallRuleManager(store.firewallRules(), store.networks());
    }

    /** The {@link StorageProvider} for {@code --provider} — storage is provider-scoped, unlike Network/Project/IAM. */
    protected final StorageProvider storageProvider() {
        return root().storageProviderRegistry().get(requireProviderName());
    }

    /** The {@link LoadBalancerProvider} for {@code --provider} — resolved against {@code virtualization.loadbalancers.*}, a separate namespace from the VM/storage {@code --provider}. */
    protected final LoadBalancerProvider loadBalancerProvider() {
        return root().loadBalancerProviderRegistry().get(requireProviderName());
    }

    /**
     * Bootstraps a default admin (printing its one-time password to stderr) if the IAM store is
     * still empty, then authenticates the presented {@code --api-token}/{@code
     * VIRTUALIZATION_API_TOKEN}.
     */
    protected final User authenticatedUser() {
        JsonFileIamStore store = iamStore();
        Bootstrap.ensureAdmin(store.users(), store.roles(), store.apiCredentials()).ifPresent(result -> {
            err().println("Created default admin user '" + Bootstrap.DEFAULT_ADMIN_USERNAME + "'.");
            err().println("Password: " + result.password());
            err().println("API token: " + result.apiToken());
            err().println("Save these now — they will not be shown again.");
        });
        String token = ApiCredentials.resolve(root().apiToken);
        return new DefaultApiCredentialManager(store.apiCredentials(), store.users()).authenticate(token);
    }

    /** No-op unless {@code virtualization.iam.enabled: true} is set in the config file. */
    protected final void requirePermission(String permission) {
        if (!IamConfigLoader.isEnabled(root().resolveConfigPath())) {
            return;
        }
        User principal = authenticatedUser();
        if (!authorizationService().hasPermission(principal, Permission.parse(permission))) {
            throw new AuthorizationException("Missing required permission '" + permission + "'");
        }
    }
}
