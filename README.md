
# Virtualization SDK
<img width="1254" height="1254" alt="ChatGPT Image Aug 26, 2026, 03_20_57 PM" src="https://github.com/user-attachments/assets/25a064df-9ad5-4b79-a2e3-74fbea40a914" />
A Java 25 / Maven / Spring Boot 4 SDK for managing virtual machines across **Proxmox VE**,
**Incus**, and **QEMU** (via QMP) with a provider-neutral core, a standalone CLI, and a Spring
Boot starter, all producible as self-contained executable JARs. A parallel, equally
provider-neutral layer adds domain, DNS and TLS certificate management (ACME DNS-01) on top,
independent of any single virtualization or DNS backend.

```bash
virtualization --provider production vm list --output json
```

## Contents

- [Architecture](#architecture)
- [Requirements](#requirements)
- [Building](#building)
- [Running the CLI](#running-the-cli)
- [Spring Boot integration](#spring-boot-integration)
- [Provider configuration](#provider-configuration)
  - [Proxmox](#proxmox)
  - [Incus](#incus)
  - [QEMU / QMP](#qemu--qmp)
- [Authentication architecture](#authentication-architecture)
- [Provider architecture](#provider-architecture)
- [Image management](#image-management)
- [VPS management](#vps-management)
- [Domain, DNS & certificate management](#domain-dns--certificate-management)
- [Extension guide](#extension-guide)
- [Testing](#testing)
- [Known limitations](#known-limitations)

## Architecture

![Architecture image](./Architecture.png)

The dependency direction is strict and enforced by the module structure itself: `core` never
depends on a provider, Spring, or Picocli. Providers depend only on `core`. The CLI and the
Spring Boot starter are consumers of `core` + the provider modules, they never depend on each
other.

### Module map

| Module | What it is |
|---|---|
| `virtualization-core` | Provider-neutral domain model, `VirtualizationProvider` interface, capability model, async `Operation`, exception hierarchy — plus the parallel image-management model (`ImageProvider`, `Image`, `WorkloadSpec`, ...), see [Image management](#image-management). Pure Java. |
| `virtualization-vps` | Provider-neutral VPS layer (`Vps`, `VpsSpec`, `VpsManager`) composed above `VirtualizationProvider` + `ImageProvider` — see [VPS management](#vps-management). Depends only on `virtualization-core`, no provider, no Spring. |
| `virtualization-dns` | Provider-neutral DNS abstraction (`DnsProvider`, `DnsZone`, `DnsRecord`) — no concrete backend (Cloudflare, Route53, ...) is hard-coded here or anywhere else in this SDK. Pure Java. |
| `virtualization-dns-mock` | `InMemoryDnsProvider` — a real, shippable `DnsProvider` for local dev/testing, not a real Cloudflare/Route53 client. |
| `virtualization-domain` | Provider-neutral domain registration + DNS record management (`Domain`, `DomainManager`) composed above `virtualization-dns`. Depends only on `virtualization-core` + `virtualization-dns`. |
| `virtualization-certificate` | Provider-neutral certificate lifecycle (`Certificate`, `CertificateManager`, `AcmeProvider`, `CertificateStore`) — no concrete CA (Let's Encrypt, ZeroSSL, an internal CA) is hard-coded here either. Depends only on `virtualization-core`. |
| `virtualization-acme-mock` | `Dns01AcmeProvider` — a real, tested DNS-01 challenge/validate/cleanup workflow over any `DnsProvider`, but not a real ACME wire client against a real CA. |
| `virtualization-provisioning` | `VpsProvisioningService` — orchestrates `VpsManager` + `DomainManager` + `CertificateManager` for one combined "create a VPS, optionally point DNS at it, optionally get it a TLS certificate" call. A library API, not (yet) wired into the CLI or Spring — see [Domain, DNS & certificate management](#domain-dns--certificate-management). |
| `virtualization-deployment` | `CertificateDeployer` — gets a certificate's material onto a deployment target and (optionally) triggers a reverse-proxy reload. `LocalFilesystemCertificateDeployer` is the only implementation, local-filesystem only, not a real SSH-based remote deployer. |
| `virtualization-proxmox` | `VirtualizationProvider` for Proxmox VE, over its HTTPS REST API (API token auth). No `ImageProvider` yet. |
| `virtualization-incus` | `VirtualizationProvider` **and** `ImageProvider` for Incus, over its REST API (mutual TLS). |
| `virtualization-qemu` | `VirtualizationProvider` for a single running QEMU process, over QMP (Unix socket or TCP). No `ImageProvider`. |
| `virtualization-cli` | Standalone Picocli CLI (`virtualization` command), packaged as a self-contained executable JAR. |
| `virtualization-spring-boot-autoconfigure` | `@ConfigurationProperties` + auto-configuration wiring providers (workload and image) from YAML into Spring beans, plus an optional REST API (`.web` package, needs `spring-boot-starter-web`). |
| `virtualization-spring-boot-starter` | Empty starter POM pulling in the autoconfigure module, by Spring Boot convention. |
| `virtualization-spring-security` | Optional OIDC login bridge for your application's own endpoints. Not a dependency of the base starter. |
| `virtualization-example-spring` | Minimal runnable Spring Boot app demonstrating the starter. |

## Requirements

- **Java 25** (the build fails fast on anything older: `maven.compiler.release=25`)
- **Maven** or just the bundled wrapper, no local Maven install required:
  ```bash
  ./mvnw --version
  ```

## Building

From a clean checkout:

```bash
./mvnw clean verify
```

This compiles every module, runs the full test suite (245+ tests), and packages every module
including the CLI's self-contained JAR and the Spring example's repackaged JAR. **No external
services are required**: Proxmox, Incus, QEMU, and Docker are never touched by the default test
suite. Every provider module tests against an in-process fake server instead (a plain-HTTP
`HttpServer` for Proxmox/Incus, a fake QMP server over TCP and a real loopback AF_UNIX socket for
QEMU).

```bash
./mvnw -o clean verify   # fully offline, once dependencies are cached
```

## Running the CLI

Build once, then run the packaged JAR directly, it bundles every runtime dependency, no Maven
or classpath setup needed:

```bash
./mvnw -pl virtualization-cli -am package -DskipTests
java -jar virtualization-cli/target/virtualization-cli.jar --help
```

```
Usage: virtualization [-hV] [--config=<configPath>] [--output=<output>]
                      [--provider=<provider>] [COMMAND]
Manage virtual machines and images across Proxmox, Incus and QEMU providers.
      --config=<configPath>
                          Path to the provider configuration YAML file
                            (default: ~/.virtualization/config.yaml).
  -h, --help              Show this help message and exit.
      --output=<output>   Output format: TABLE, JSON, YAML (default: table).
      --provider=<provider>
                          Name of the configured provider to use.
  -V, --version           Print version information and exit.
Commands:
  provider  Inspect configured providers.
  vm        Manage virtual machines.
  image     Manage images.
  workload  Manage workloads (containers and VMs) created from images.
  vps       Manage VPSs: provider-neutral image + compute + storage + network + lifecycle.
```

### Configuring the CLI

The CLI reads provider configuration from `~/.virtualization/config.yaml` by default (override
with `--config <path>`). The shape is identical to the Spring Boot starter's configuration. See
[Provider configuration](#provider-configuration) below for the full field reference per
provider type.

```yaml
# ~/.virtualization/config.yaml
virtualization:
  providers:
    production:
      type: proxmox
      endpoint: https://pve.example.com:8006
      token-id: root@pam!sdk
      token-secret: ${PROXMOX_TOKEN_SECRET}
    containers:
      type: incus
      endpoint: https://incus.example.com:8443
      client-cert-path: /etc/virtualization/incus-client-cert.pem
      client-key-path: /etc/virtualization/incus-client-key.pem
    local:
      type: qemu
      socket: /run/qemu/myvm.qmp
```

`${VAR}` references are substituted from the environment at load time; an unset variable fails
config loading with a clear error rather than silently embedding the literal `${VAR}` string.

### Commands

```
virtualization provider list [--output table|json|yaml]

virtualization vm list      --provider <name> [--output table|json|yaml]
virtualization vm get <id>  --provider <name> [--output table|json|yaml]

virtualization vm start    <id> --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vm stop     <id> --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vm reboot   <id> --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vm shutdown <id> --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vm destroy  <id> --provider <name> [--wait|--no-wait] [--timeout <seconds>]

virtualization image list                    --provider <name> [--output table|json|yaml]
virtualization image search [<query>]        --provider <name> [--distribution <d>] [--architecture <a>] [--os <os>] [--version <v>] [--type CONTAINER|VIRTUAL_MACHINE] [--remote <r>]
virtualization image get     <reference>     --provider <name>
virtualization image pull    <reference>     --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization image download <reference>    --provider <name> --file <path>
virtualization image import  <file>          --provider <name> [--wait|--no-wait] [--timeout <seconds>]

virtualization workload create --provider <name> --image <reference> --name <name> \
    [--type CONTAINER|VIRTUAL_MACHINE] [--cpu <n>] [--memory <mb>] \
    [--storage <spec>]... [--network <name>]... [--profile <p>] [--project <p>] \
    [--wait|--no-wait] [--timeout <seconds>]

virtualization vps create --provider <name> --image <reference> --name <name> \
    [--type CONTAINER|VIRTUAL_MACHINE] [--cpu <n>] [--memory-mb <mb>] [--disk-mb <mb>] \
    [--storage-pool <p>] [--volume-type <t>] \
    [--network <name>] [--ipv4 <addr>] [--ipv6 <addr>] [--hostname <name>] \
    [--ssh-key <key>]... [--cloud-init <doc>] [--location <l>] [--project <p>] \
    [--idempotency-key <key>] [--wait|--no-wait] [--timeout <seconds>]

virtualization vps list                        --provider <name> [--output table|json|yaml]
virtualization vps get       <id>               --provider <name> [--output table|json|yaml]
virtualization vps start     <id>               --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vps stop      <id>               --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vps restart   <id>               --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vps shutdown  <id>               --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vps destroy   <id>               --provider <name> [--wait|--no-wait] [--timeout <seconds>]
virtualization vps rebuild   <id> --image <reference> --provider <name> [--wait|--no-wait] [--timeout <seconds>]

virtualization domain list                                          [--output table|json|yaml]
virtualization domain get <domain>                                  [--output table|json|yaml]

virtualization dns zone list   --dns-provider <name>                [--output table|json|yaml]
virtualization dns record list   <zone> --dns-provider <name>       [--output table|json|yaml]
virtualization dns record create <zone> --dns-provider <name> --name <name> --type A|AAAA|CNAME|TXT|MX --value <value> [--ttl <seconds>] [--priority <n>]
virtualization dns record delete <zone> <id> --dns-provider <name>

virtualization certificate list                                     [--output table|json|yaml]
virtualization certificate get     <id>                             [--output table|json|yaml]
virtualization certificate request --domain <domain>... --provider <issuer> [--dns-provider <name>] [--challenge DNS_01] [--passphrase <pass>]
virtualization certificate renew   <id>
virtualization certificate revoke  <id>
virtualization certificate export  <id> [--cert <path>] [--chain <path>] [--private-key <path> --include-private-key --yes] [--passphrase <pass>]
```

VPS state doesn't live in memory across CLI invocations (there is no long-lived process) — it's
persisted to a JSON file next to the config file (`vps.json`, same directory as `--config`, or
`~/.virtualization/vps.json` by default), so `vps create` in one shell and `vps list`/`get` in the
next actually see each other. DNS records and certificate metadata follow the same pattern:
`dns record create`/`delete` persist to `dns-<provider-name>.json`, and `certificate request` (and
onward) to `certificates.json` — both next to `--config`. `domain list`/`domain get` are always
empty/`404`, though: nothing in this SDK (CLI, REST, or otherwise) yet calls `DomainManager.register`
— see [Known limitations](#known-limitations).

Certificate *material* (private key, cert, chain) is never written as plaintext: `certificate
request`/`export` encrypt/decrypt it (AES/GCM, PBKDF2-derived key) in `certificates.enc`, next to
`--config`, using a passphrase from `--passphrase` or the `VIRTUALIZATION_CERTIFICATE_PASSPHRASE`
environment variable (preferred — a flag value can land in shell history). `certificate list/get/
renew/revoke` never touch that file and need no passphrase at all. `certificate export
--private-key <path>` additionally requires **both** `--include-private-key` and `--yes` — omitting
either refuses (exit code `2`) before any key material is read.

`<reference>` is `remote:identifier` (e.g. `images:ubuntu/24.04`) or a bare identifier when the
provider has no remote concept. `image delete` and `image aliases` are not implemented — see
[Known limitations](#known-limitations).

`--provider` and `--output` are recognized at any position : both of these work identically:

```bash
virtualization --provider production vm list --output json
virtualization vm list --provider production --output json
```

Machine-readable output (`--output json` / `--output yaml`) always goes to **stdout only**;
errors and any logging go to **stderr**, so `vm list --output json | jq .` is always safe to
pipe. Exit codes are meaningful:

| Code | Meaning |
|---|---|
| `0` | success |
| `1` | other SDK-level failure |
| `2` | usage error (bad arguments, unknown command, missing `--provider`) |
| `3` | resource not found |
| `4` | provider does not support the requested capability |
| `5` | authentication/authorization failure against the provider |
| `6` | could not reach the provider (network/TLS failure) |
| `7` | invalid provider configuration |

## Spring Boot integration

Add the starter:

```xml
<dependency>
  <groupId>io.virtualization.sdk</groupId>
  <artifactId>virtualization-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Configure providers in `application.yml` (same shape as the CLI's config file):

```yaml
virtualization:
  providers:
    production:
      type: proxmox
      endpoint: https://pve.example.com:8006
      token-id: root@pam!sdk
      token-secret: ${PROXMOX_TOKEN_SECRET}
```

Inject `VirtualizationClient` wherever you need it. `ProviderRegistry`, `ImageProviderRegistry`
and `VirtualizationClient` beans are all `@ConditionalOnMissingBean`, so defining your own bean of
any of the three (e.g. to build providers programmatically instead of from YAML) fully overrides
the auto-configuration for that bean:

```java
@Service
public class VmService {

    private final VirtualizationClient client;

    public VmService(VirtualizationClient client) {
        this.client = client;
    }

    public List<VirtualMachine> list() {
        return client.provider("production").listVirtualMachines();
    }
}
```

`client.images(name)` gives you the `ImageProvider` side the same way — same bean, independent
accessor, backed by its own `ImageProviderRegistry` (only `incus` providers get one so far):

```java
@Service
public class ImageService {

    private final VirtualizationClient client;

    public ImageService(VirtualizationClient client) {
        this.client = client;
    }

    public List<Image> list() {
        return client.images("containers").list();
    }
}
```

A third bean, `VpsManager`, is composed on top of `VirtualizationProvider` + `ImageProvider` — see
[VPS management](#vps-management). Unlike `VirtualizationClient` it's **not** always present: it
only gets created once `virtualization.vps.provider` names one of your configured providers, so
inject it as `Optional<VpsManager>` rather than a hard dependency if your app should still start
without VPS support configured:

```yaml
virtualization:
  vps:
    provider: containers   # must name an entry under virtualization.providers
```

```java
@Service
public class VpsService {

    private final Optional<VpsManager> manager;

    public VpsService(Optional<VpsManager> manager) {
        this.manager = manager;
    }

    public List<Vps> list() {
        return manager.map(VpsManager::list).orElseGet(List::of);
    }
}
```

A complete runnable example lives in `virtualization-example-spring/` (`VmService` +
`ImageService` + `VpsService`, one `CommandLineRunner` each), run it with:

```bash
./mvnw -pl virtualization-example-spring -am spring-boot:run
```

DNS/domain/certificate beans follow the same "off unless configured" stance as `VpsManager`, but
opt in via an explicit flag rather than "some provider is named" — `DnsProviderRegistry` (and any
`AcmeProviderRegistry`) is always created, even empty, the same way `ProviderRegistry` is, but
`DomainManager`/`CertificateManager` beans (and their controllers) only appear when
`virtualization.domains.enabled=true` / `virtualization.certificates.enabled=true`:

```yaml
virtualization:
  dns:
    providers:
      cloudflare:
        type: mock             # only type today — see Known limitations
        zones: [example.com]
  domains:
    enabled: true
  certificates:
    enabled: true
    providers:
      letsencrypt:
        type: mock              # only type today — see Known limitations
        dns-provider: cloudflare # must name an entry under virtualization.dns.providers
```

```java
@Service
public class DomainService {

    private final DomainManager domainManager;

    public DomainService(DomainManager domainManager) {
        this.domainManager = domainManager;
    }

    public Domain register(String name) {
        return domainManager.register(name);
    }
}
```

Inject `DomainManager`/`CertificateManager` directly (not `Optional<...>`) if your app always
enables them; inject `Optional<DomainManager>`/`Optional<CertificateManager>` the same way
`VpsService` does for `VpsManager` if support is conditional on configuration.

### Optional: REST API

Add `spring-boot-starter-web` to your application (the autoconfigure module only depends on it
*optionally*, so a non-web app never pulls in a servlet container) and `/api/v1/images` +
`/api/v1/workloads` are exposed automatically — see [Image management](#image-management) for the
full endpoint list. `/api/v1/vps` additionally activates once `virtualization.vps.provider` is set
(the `VpsController` bean is `@ConditionalOnBean(VpsManager.class)`) — see
[VPS management](#vps-management). `/api/v1/domains` and `/api/v1/certificates` activate the same
way, gated on `virtualization.domains.enabled`/`virtualization.certificates.enabled` — see
[Domain, DNS & certificate management](#domain-dns--certificate-management). Set
`virtualization.web.enabled=false` to keep a web app's own endpoints from including this API
surface.

### Optional: OIDC login for your application

`virtualization-spring-security` is a **separate, optional** module, it is never pulled in by
the base starter. It secures your *application's own* HTTP endpoints (Okta or any other
standards-compliant OIDC provider); it has no knowledge of, and never touches, provider
credentials. Add it, plus your own `spring.security.oauth2.client.registration.*` configuration
and a `ClaimsToRoleMapper` bean if you want OIDC claims mapped onto Spring Security roles:

```java
@Bean
ClaimsToRoleMapper claimsToRoleMapper() {
    return claims -> claims.get("groups") instanceof List<?> groups
            ? groups.stream().map(g -> "ROLE_" + g).toList()
            : List.of();
}
```

See [Authentication architecture](#authentication-architecture) for why this is deliberately
disconnected from provider authentication.

## Provider configuration

Every provider entry lives under `virtualization.providers.<name>` (CLI YAML file or Spring
`application.yml`, identical shape either way) and always has a `type` discriminator.

### Proxmox

```yaml
virtualization:
  providers:
    production:
      type: proxmox
      endpoint: https://pve.example.com:8006   # required
      token-id: root@pam!sdk                    # required — user@realm!tokenName
      token-secret: ${PROXMOX_TOKEN_SECRET}      # required, never logged
      verify-ssl: true                           # optional, default true
```

Authenticates with a Proxmox **API token** (`Authorization: PVEAPIToken=...`) over HTTPS via
`java.net.http.HttpClient`, no `pvesh`/`qm`/`pct` shell-outs. Supports QEMU VM listing and
lifecycle (start/stop/reboot/shutdown/destroy); operations are driven to completion by polling
the Proxmox task API. LXC containers, snapshots, storage and networks are reachable through the
Proxmox REST API and mapped by `virtualization-core`'s domain model, but `VirtualizationProvider`
itself has no methods for them yet — see [Extension guide](#extension-guide).

### Incus

```yaml
virtualization:
  providers:
    containers:
      type: incus
      endpoint: https://incus.example.com:8443     # required
      client-cert-path: /path/to/client-cert.pem     # required — PEM X.509 certificate
      client-key-path: /path/to/client-key.pem       # required — PEM PKCS#8 private key
      verify-ssl: true                                # optional, default true
```

Authenticates with **mutual TLS** : Incus has no bearer-token header; the trusted client
certificate presented during the TLS handshake *is* the credential. The private key must be
PKCS#8 (`-----BEGIN PRIVATE KEY-----`); convert a legacy PKCS#1 key with:

```bash
openssl pkcs8 -topk8 -nocrypt -in key.pem -out key8.pem
```

Only Incus instances of type `virtual-machine` are exposed through `listVirtualMachines()` today
— see [Extension guide](#extension-guide) for containers/profiles/networks/storage. Images are
covered separately: Incus is the only provider with an `ImageProvider` so far — see
[Image management](#image-management).

### QEMU / QMP

```yaml
virtualization:
  providers:
    local:
      type: qemu
      socket: /run/qemu/myvm.qmp   # a Unix domain socket, OR:
      # host: 127.0.0.1            # a TCP endpoint (mutually exclusive with `socket`)
      # port: 4444
```

Talks **QMP** directly (QEMU's own JSON control protocol) — never shells out to
`qemu-system-x86_64`. A QMP socket controls exactly one already-running QEMU process (there is no
cluster or VM-catalog concept), so one `qemu` provider entry manages exactly one VM, identified
by the entry's own name (`local` above). Supported QMP commands: `query-status`, `cont`, `stop`,
`system_reset`, `system_powerdown`, `query-cpus`. `destroy` is intentionally **unsupported**
(`UnsupportedCapabilityException`). QMP has no VM-teardown concept; tearing down the process is
outside what the protocol itself exposes.

Lifecycle operations complete as soon as QEMU **acknowledges** the QMP command, not when a
corresponding guest-level event fires (e.g. the guest actually finishing an ACPI shutdown) — a
guest can ignore that request forever, exactly like `virsh shutdown`.

## Authentication architecture

Two authentication layers are completely independent, by design:

```
     Human user
        |
        | OIDC / Okta login  (virtualization-spring-security, optional)
        v
  Your Spring Boot application
        |
        | virtualization.providers.<name>.*  (static, app-wide)
        |
        v
  Proxmox API token / Incus client certificate / QEMU QMP socket
```

An authenticated user's OIDC access token is **never** forwarded to a provider backend, and a
provider credential is never derived from who is logged into the application. `ProxmoxCredentials`
and `IncusTlsCredentials` are provider-specific, live in their own provider modules, and are
configured once for the whole application, not per user. If you need per-user provider access
control, build it as an authorization layer in your own application (e.g. checking the
authenticated user's roles before calling `client.provider(name)`), `virtualization-core` has no
opinion on it.

## Provider architecture

```java
public interface VirtualizationProvider {
    ProviderType type();
    ProviderCapabilities capabilities();
    List<VirtualMachine> listVirtualMachines();
    VirtualMachine getVirtualMachine(String id);
    Operation start(String id);
    Operation stop(String id);
    Operation reboot(String id);
    Operation shutdown(String id);
    Operation destroy(String id);
}
```

Not every provider supports every operation: check `capabilities()` first, or catch
`UnsupportedCapabilityException` (QEMU's lack of `destroy` is the concrete example above).

Asynchronous operations are represented explicitly, never hidden behind a blocking call:

```java
Operation operation = client.provider("production").start("100");
OperationStatus status = operation.await(Duration.ofMinutes(2));   // never throws on FAILED
if (status == OperationStatus.FAILED) {
    operation.error().ifPresent(e -> log.warn("start failed: {}", e.getMessage()));
}
```

Provider modules drive an `Operation` to completion via the producer-side `OperationHandle` —
consumers only ever see the read-only `Operation` view, so a caller holding a `VirtualMachine`'s
operation result cannot itself call `complete()`/`fail()`.

Every provider translates its own failures into the shared, provider-neutral exception hierarchy
(`AuthenticationException`, `AuthorizationException`, `ConnectionException`,
`ResourceNotFoundException`, `OperationException`, `UnsupportedCapabilityException`,
`ConfigurationException`) rather than leaking HTTP status codes or QMP error payloads — the
original cause is always preserved, secrets never are.

## Image management

A second, independent provider abstraction sits alongside `VirtualizationProvider`:

```
                VirtualizationClient
                       │
         ┌─────────────┴─────────────┐
         │                           │
         ▼                           ▼
   VirtualizationProvider       ImageProvider
   (workloads, per-name          (images, per-name
    ProviderRegistry)             ImageProviderRegistry)
```

`VirtualizationClient.provider(name)` and `.images(name)` are backed by two separate named
registries — an application injects either independently, or both. Only `virtualization-incus`
implements `ImageProvider` today (`IncusImageProvider`); Proxmox and QEMU have none yet.

### Core model (`virtualization-core`, `io.virtualization.sdk.core.image`)

| Type | What it is |
|---|---|
| `Image` | Provider-neutral image record: id, name, type, architecture, os/distribution/version, size, `createdAt`, free-form `metadata`. |
| `ImageId` | The identity a provider assigns to an *already-resolved* image (a fingerprint, a digest). |
| `ImageReference` | What a caller supplies to *locate* an image: `provider` + optional `remote` + `identifier` (e.g. `incus` / `images` / `ubuntu/24.04`). Not the same thing as `ImageId` — a reference may be an alias that still needs resolving. |
| `ImageType` | `CONTAINER`, `VIRTUAL_MACHINE`, `DISK`, `ISO`, `OCI`, `UNKNOWN`. |
| `ImageQuery` | Builder-style search filter: name, alias, architecture, os, distribution, version, type, remote, provider — all optional. |
| `ImageCapability` / `ImageCapabilities` | Same pattern as `Capability`/`ProviderCapabilities`: `LIST`, `INSPECT`, `SEARCH`, `PULL`, `DOWNLOAD`, `UPLOAD`, `DELETE`, `INSTANTIATE`, `SNAPSHOT`, `PUBLISH`. |
| `ImageSource` | Sealed: `LocalFileImageSource`, `InputStreamImageSource`, `URLImageSource`, `RemoteImageSource` — what `importImage` reads from. |
| `ImageDownload` | Streamed export handle (`stream()`, `contentLength()`, `checksum()`, `mediaType()`), `AutoCloseable`. |
| `ImagePullOperation` / `ImageImportOperation` / `CreateWorkloadOperation` | Extend the existing `Operation` (status/progress/error/await reused as-is) with what's specific to each: byte counts for pull/import, the resulting `Image` for import, the resulting workload id for create. |
| `WorkloadSpec` | Builder-style: name, type, image, architecture, `ComputeResources` (reused from the workload side), storage/networks/mounts/ports (free-form strings — no typed structure yet, nothing needs one), environment/metadata/labels, and a `ProviderOptions` escape hatch. |
| `ImageAvailabilityPolicy` | `REQUIRE_LOCAL`, `PULL_IF_MISSING` (default), `ALWAYS_REFRESH` — governs `VirtualizationProvider.createFromImage`. |

```java
public interface ImageProvider {
    String name();
    ImageCapabilities capabilities();
    List<Image> list();
    Optional<Image> get(ImageReference reference);
    List<Image> search(ImageQuery query);
    ImagePullOperation pull(ImageReference reference);              // default: throws UnsupportedCapabilityException
    ImageDownload download(ImageReference reference);                // default: throws UnsupportedCapabilityException
    void download(ImageReference reference, OutputStream out);       // default: streams download() into out
    ImageImportOperation importImage(ImageSource source);            // default: throws UnsupportedCapabilityException
}
```

`pull` and `download` are deliberately different operations: `pull` retrieves an image from a
remote **into the provider's own store** (nothing crosses back to the JVM); `download` **exports**
image bytes to the caller, streamed, never buffered in full. `VirtualizationProvider` gains the
matching create-from-image half:

```java
CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec);                          // PULL_IF_MISSING
CreateWorkloadOperation createFromImage(ImageReference image, WorkloadSpec spec, ImageAvailabilityPolicy policy);
```

Both default to `UnsupportedCapabilityException`, same convention as the rest of
`VirtualizationProvider`.

### Incus implementation

`IncusImageProvider` covers `LIST`/`INSPECT`/`SEARCH`/`PULL`/`DOWNLOAD`/`UPLOAD`/`INSTANTIATE`
(not `DELETE`, not `SNAPSHOT`/`PUBLISH`). A few things worth knowing before relying on it:

- **Scoped to one remote.** An `IncusImageProvider` instance serves the local store of whatever
  Incus server its `IncusApiClient` talks to (default remote name `"local"`). Reaching another
  remote means talking to a different server — register one `IncusImageProvider` per remote you
  need, the same way multiple `IncusProvider`s get registered under different names.
- **`pull` never speaks simplestreams.** Incus's own built-in default remotes (`images:`,
  `ubuntu:`, `ubuntu-daily:`) use the simplestreams protocol, which this SDK does not implement.
  `pull` instead submits `{server, protocol, alias}` straight to the connected Incus server's
  `POST /1.0/images`, and **the Incus server itself** does the simplestreams fetch. One consequence:
  browsing/searching those public catalogs *before* a pull isn't supported — you pull by a known
  alias, you don't discover one through this SDK.
- **`download`'s checksum is the fingerprint**, which is accurate for a unified (single-file)
  image export — Incus computes the fingerprint as the export's own SHA-256. A split
  metadata+rootfs export (which this provider doesn't distinguish) would not match.
- **`ImageAvailabilityPolicy` in `createFromImage`**: same remote as the provider's own → resolved
  or `ResourceNotFoundException`, no policy changes that. A different, known remote:
  `REQUIRE_LOCAL` rejects outright; `PULL_IF_MISSING` (default) hands `{server, protocol, alias}`
  straight to the instance-create call and lets Incus fetch-or-reuse-cache itself, no extra round
  trip; `ALWAYS_REFRESH` calls `pull` explicitly first, then creates from the freshly-resolved
  fingerprint.

### CLI

See [Commands](#commands) above for the full `image`/`workload` command reference.

### REST API

Exposed by `virtualization-spring-boot-autoconfigure` when `spring-boot-starter-web` is present
(see [Optional: REST API](#optional-rest-api)):

| Method & path | |
|---|---|
| `GET /api/v1/images?provider=<name>` | list |
| `GET /api/v1/images/search?provider=<name>&q=...&distribution=...&...` | search |
| `GET /api/v1/images/{id}?provider=<name>` | get by reference (`{id}` = `remote:identifier`, may contain `/`) |
| `GET /api/v1/images/download/{id}?provider=<name>` | streamed export — note `download` comes *before* `{id}`, not after: Spring's `PathPattern` matcher only lets a multi-segment `{*id}` capture be the terminal element of a mapping |
| `POST /api/v1/images/pull` | body `{"provider","remote","identifier"}` |
| `POST /api/v1/images/import?provider=<name>` | raw `application/octet-stream` body, streamed straight into the provider |
| `POST /api/v1/workloads` | body `{"provider","name","type","image":{"remote","name"},"cpu","memoryMb","storage","networks","environment","providerOptions"}` |

`provider` is a required query param / JSON field everywhere, the same role it plays in the CLI.
`pull`/`import`/`workloads` are async `Operation`s at the SDK level but exposed **synchronously**
here: the controller awaits completion (5 minute bound) and returns the terminal state
(`{"id","status","progress","error"}`, plus the imported `Image`/created workload id where
relevant) in the response body. There is no polling endpoint yet. `DELETE /api/v1/images/{id}` is
not implemented (see [Known limitations](#known-limitations)).

## VPS management

A third, higher-level abstraction sits above both `VirtualizationProvider` and `ImageProvider`:

```
                         VpsManager
                             │
                     VpsProvisioner (DefaultVpsProvisioner)
                             │
              ┌──────────────┴──────────────┐
              ▼                              ▼
      VirtualizationProvider              ImageProvider
      (create/start/stop/.../destroy)     (fail-fast image existence check)
```

A `Vps` composes an image + compute + storage + network + lifecycle into one orchestrated
resource, so a caller provisions "a VPS" instead of hand-assembling a `WorkloadSpec` and calling
`createFromImage` itself. `virtualization-vps` depends only on `virtualization-core` — no provider
module, no Spring, no Picocli; `DefaultVpsProvisioner` is the only place it touches
`VirtualizationProvider`/`ImageProvider`, and only through those two interfaces, never a concrete
provider.

### Core model (`virtualization-vps`, `io.virtualization.sdk.vps`)

| Type | What it is |
|---|---|
| `Vps` | Provider-neutral record: id, name, state, type, image, compute, storage, network, the originating `spec` (kept for rebuild/audit), `provider`/`project`/`workloadId` (`null` until provisioned), timestamps (`createdAt`/`updatedAt` always set, `startedAt`/`stoppedAt`/`destroyedAt` set on first transition). |
| `VpsId` | `"vps-" + UUID`, generated by the manager — independent of any provider's own instance name/id. |
| `VpsSpec` | Builder-style, mirrors `WorkloadSpec`'s shape: `name`/`image` required, `type` (default `VIRTUAL_MACHINE`), `.cpu(int)`/`.memory(DataSize)` sugar *or* `.compute(ComputeResources)` whole-object (whole-object wins if both used — same rule for `.disk()`/`.storagePool()`/`.volumeType()` vs `.storage(StorageConfiguration)`), `sshPublicKeys`, `cloudInit`, `network`, `metadata`/`labels`, `location`, `project`, `idempotencyKey`. |
| `DataSize` | Bytes-based value type (`ofGigabytes`/`ofMegabytes`/`ofBytes`, `toMegabytes()`) — sizing sugar on `VpsSpec`; never leaks into `virtualization-core` (`ComputeResources.memoryMb` stays a plain `long`). |
| `StorageConfiguration` | `rootDisk` (`DataSize`, required) + optional `storagePool`/`volumeType`. |
| `NetworkConfiguration` | `network`/`ipv4`/`ipv6`/`hostname`, all nullable — `UNSPECIFIED` = pure DHCP. |
| `VpsState` | `PROVISIONING, STOPPED, STARTING, RUNNING, READY, STOPPING, REBUILDING, ERROR, DESTROYING, DESTROYED` with its own `canTransitionTo` edge table; `DESTROYED` is terminal. `destroy()` is soft-delete — the row stays, state becomes `DESTROYED`. |
| `VpsRepository` / `InMemoryVpsRepository` | Storage seam. The only implementation in `virtualization-vps` itself is in-memory; the CLI supplies its own JSON-file-backed one (below) since a CLI process doesn't stay alive between commands. |
| `VpsProvisioner` / `DefaultVpsProvisioner` | The provider-facing seam: `create`/`rebuild` (async, virtual-thread-backed) and `start`/`stop`/`restart`/`shutdown`/`destroy` (delegate straight to `VirtualizationProvider` by the VPS's `workloadId`). `DefaultVpsProvisioner` composes any `VirtualizationProvider` + `ImageProvider` pair — provider-neutral, no Incus (or anyone else) dependency here. |
| `VpsManager` / `DefaultVpsManager` | Owns the state machine, idempotency (`ConcurrentHashMap#computeIfAbsent` on `VpsSpec.idempotencyKey()` — in-process, single-JVM, not a distributed lock), and lazy reconciliation: no background threads, an in-flight `VpsProvisioner` operation is only resolved into the repository the next time `get()`/`list()` is called for that id. |
| `VpsReadinessChecker` / `TcpReadinessChecker` | Consulted before flipping `PROVISIONING`/`REBUILDING` to `READY` — default `alwaysReady()` (trusts the provisioner's own success signal); `TcpReadinessChecker` does a real TCP connect (default port 22) against the VPS's static `ipv4`/`ipv6`, up to 3 attempts 200ms apart, landing in `ERROR` (not silently staying `READY`) if it never connects. A VPS with no known static address (DHCP) can't be probed this way and is treated as ready. |

Legal state transitions, enforced by `DefaultVpsManager` (narrower than `VpsState.canTransitionTo`
alone — `start` and `restart` both target `STARTING` but from disjoint legal sources):

| method | legal from | transient | terminal (success) |
|---|---|---|---|
| `create` | *(new)* | `PROVISIONING` | `READY` |
| `start` | `STOPPED` | `STARTING` | `RUNNING` |
| `stop` | `RUNNING`, `READY` | `STOPPING` | `STOPPED` |
| `restart` | `RUNNING`, `READY` | `STARTING` | `RUNNING` |
| `shutdown` | `RUNNING`, `READY` | `STOPPING` | `STOPPED` |
| `destroy` | `READY`, `STOPPED`, `RUNNING`, `ERROR` | `DESTROYING` | `DESTROYED` |
| `rebuild` | `READY`, `STOPPED`, `ERROR` | `REBUILDING` | `READY` |

Any other source state raises `InvalidVpsStateException` (mapped to HTTP `409` in the REST API,
the generic SDK-failure exit code `1` in the CLI). `rebuild` destroys the current workload (if
any) and creates a fresh one from the new image, keeping the same `VpsId`/name/spec otherwise.

### Incus provisioning

`IncusProvider.createFromImage` reads `WorkloadSpec.providerOptions()` keys that
`VpsProviderOptionKeys` documents (matched by string literal — `virtualization-incus` has no
compile dependency on `virtualization-vps`, same convention as the pre-existing `"profile"` key):

- `sshPublicKeys` (`List<String>`) / `hostname` (`String`) synthesize a minimal `#cloud-config`
  document set as the `cloud-init.user-data` instance config key; an explicit `cloudInit`
  providerOption is used verbatim instead and wins outright. Values are YAML-quoted before
  embedding — they're caller-controlled and land in a document cloud-init runs with root privilege
  inside the guest, so unescaped embedding would let a crafted hostname/key break out of its
  scalar and inject arbitrary directives.
- `WorkloadSpec.storage()`'s `"root:<size>"` entry and `storagePool` providerOption become an
  Incus `devices.root` disk override (`path`, `size`, `pool`); `WorkloadSpec.networks()`'s first
  entry plus `ipv4`/`ipv6` providerOptions become a `devices.eth0` nic override.
- Not applied yet: `volumeType` (no clean per-instance Incus equivalent — storage type is a
  pool-driver property, not an instance override), `location` (would need to become a `?target=`
  query parameter on the create request, not a body field), `project` (Incus project scoping
  happens at `IncusClientConfig`/`IncusApiClient` construction time, a different mechanism than a
  per-request providerOption).

### CLI

See [Commands](#commands) above for the full `vps` command reference. State persists to a JSON
file (not `InMemoryVpsRepository`) since a CLI process doesn't stay alive between invocations —
every command that awaits an operation also forces one reconciling `get()` call before the process
exits, since there's no later invocation of *that* `VpsManager` instance to do it lazily.

### REST API

Exposed by `virtualization-spring-boot-autoconfigure` when `spring-boot-starter-web` **and**
`virtualization.vps.provider` are both present (see [Optional: REST API](#optional-rest-api)):

| Method & path | |
|---|---|
| `POST /api/v1/vps` | create — body has `name`, `image:{provider,remote,name}`, and the rest of `VpsSpec`'s fields; returns `{"operation","vpsId"}` |
| `GET /api/v1/vps` | list |
| `GET /api/v1/vps/{id}` | get |
| `POST /api/v1/vps/{id}/{start,stop,restart,shutdown}` | lifecycle, returns `{"id","status","progress","error"}` |
| `DELETE /api/v1/vps/{id}` | destroy (soft — the record stays, `state` becomes `DESTROYED`) |
| `POST /api/v1/vps/{id}/rebuild` | body `{"image":{"provider","remote","name"}}`, returns `{"operation","vpsId"}` |

Same synchronous convention as `/api/v1/images`/`/api/v1/workloads`: every endpoint awaits
completion (5 minute bound) and returns the terminal state, no polling endpoint.
`InvalidVpsStateException` maps to `409 Conflict`, on top of the shared mapping every other
endpoint already uses.

`VpsManager` is bound to exactly **one** configured provider (`virtualization.vps.provider`), not
multi-provider-routed like `/api/v1/images`/`/api/v1/workloads` — see
[Known limitations](#known-limitations).

## Domain, DNS & certificate management

A second, parallel higher-level layer, independent of both the VPS layer and any single
virtualization/DNS/CA backend:

```
                                  VpsProvisioningService
                                  (virtualization-provisioning)
                                          │
                    ┌─────────────────────┼─────────────────────┐
                    ▼                     ▼                     ▼
               VpsManager            DomainManager         CertificateManager
                                          │                     │
                                    DnsProviderRegistry    AcmeProviderRegistry
                                          │                     │
                                     DnsProvider            AcmeProvider
                              (InMemoryDnsProvider,      (Dns01AcmeProvider,
                               real backend later)         real CA later)
```

`virtualization-domain` and `virtualization-certificate` never depend on Incus, Proxmox, QEMU, or
each other's concrete backend — a domain is never tied to "the Incus provider that happens to host
it", and neither Cloudflare nor Let's Encrypt is hard-coded anywhere in this SDK. Every concrete
provider shipped so far is a **mock**: a real, tested, shippable implementation of its interface
(`InMemoryDnsProvider` genuinely stores zones/records; `Dns01AcmeProvider` genuinely runs the
RFC 8555 §8.4 DNS-01 challenge/validate/cleanup workflow against whatever `DnsProvider` it's given)
but never talks to a real external service — no real Cloudflare, no real Let's Encrypt. Adding one
is a new provider module, following the same shape described in [Extension guide](#extension-guide).

### Core model

| Type | What it is |
|---|---|
| `DnsProvider` (`virtualization-dns`) | `zones()`/`getZone()`/`records()`/`createRecord()`/`updateRecord()`/`deleteRecord()` — a DNS backend. `DnsZone.name()` (e.g. `example.com`) and `DnsRecord.name()`/`DnsRecordSpec.name()` (zone-relative, e.g. `app`) are the two identities everything else is built on. |
| `DnsProviderRegistry` | Named map of `DnsProvider`s (`virtualization.dns.providers.*`), mirrors `ProviderRegistry`. |
| `InMemoryDnsProvider` (`virtualization-dns-mock`) | Zones registered via `addZone` (not part of the `DnsProvider` interface — real backends manage zone creation outside this SDK); records genuinely stored, `synchronized`, single lock per instance. |
| `Domain` / `DomainId` / `DomainManager` / `DefaultDomainManager` (`virtualization-domain`) | `register(name)` normalizes (lower-case, trailing-dot-stripped, punycode via `IDN.toASCII`) and stores a `Domain` with no DNS provider yet; `associateDnsProvider` names one `DnsProviderRegistry` entry. Record management (`createRecord`/`updateRecord`/`deleteRecord`/`listRecords`) resolves a domain to its owning zone by walking up the label chain (`app.example.com` → `example.com`) since `DnsProvider.getZone` is exact-match only — a registered domain is usually a subdomain of the zone that actually owns it. |
| `Certificate` / `CertificateId` / `CertificateStatus` (`virtualization-certificate`) | Metadata only (`REQUESTED, PENDING, ACTIVE, EXPIRING, EXPIRED, REVOKED, FAILED`) — never a private key, cert body, or chain. |
| `CertificateMaterial` / `CertificateStore` | The sensitive counterpart — PEM certificate, private key, chain — held behind a separate store, never returned by `CertificateManager` or any REST/CLI list/get response. `toString()` is fully redacted. `CertificateManager` itself never touches `CertificateStore`; only an `AcmeProvider` implementation that actually produces key material does. |
| `CertificateRequest` / `ChallengeType` | What a caller supplies to request a certificate — domains, issuer (an `AcmeProviderRegistry` key), challenge type (`DNS_01` implemented, `HTTP_01` named but not yet supported by any provider). |
| `AcmeProvider` / `AcmeProviderRegistry` | A CA (or CA-like) backend: async `request`, synchronous `get`/`renew`/`revoke`. `renew`/`revoke` take the `Certificate` the caller already has, not a bare id — an out-of-process caller (any CLI invocation, any future non-singleton deployment) can't otherwise tell a provider what a bare opaque id even refers to. |
| `Dns01AcmeProvider` (`virtualization-acme-mock`) | DNS-01 over a composed `DnsProvider`: creates `_acme-challenge.<domain>` TXT records (RFC 8555 §8.4 — always this name, apex included, never `@`) per requested domain, "validates" (no real external CA to wait on), issues, and always cleans the TXT records up afterward, success or failure. |
| `CertificateManager` / `DefaultCertificateManager` | `requestCertificate` (synchronous facade over `AcmeProvider#request`, awaits internally), `renew`/`revoke` (blocked with `IllegalStateException` on an already-`REVOKED`/`FAILED` certificate — needs explicit recovery, not a blind retry), `get`/`list`. |
| `CertificateRenewalScheduler` | On-demand batch: renews every certificate expiring within a window (default 30 days) via `CertificateManager`. Does not schedule itself — a Spring `@Scheduled` method or a cron job decides when to call `renewDue()`. One certificate's failure never blocks another's renewal in the same run. |
| `VpsProvisioningService` / `VpsProvisioningProfile` (`virtualization-provisioning`) | Orchestrates all three managers for one `provision(VpsSpec, profile)` call: `BASIC` (VPS only), `DOMAIN` (+ DNS records for `VpsSpec.domains()`, governed by `VpsSpec.dnsPolicy()` — `NONE`/`CREATE`/`CREATE_AND_UPDATE`), `HTTPS`/`WEB_SERVER` (+ a certificate request if `VpsSpec.tlsEnabled()`). DNS integration only works for a VPS with a caller-specified static IP — nothing in this SDK retrieves a provider-assigned/DHCP runtime IP yet, so a domain with no static IP simply has its DNS step skipped (logged, not failed). Not wired into the CLI or Spring yet — a library API, called programmatically. |
| `CertificateDeployer` / `LocalFilesystemCertificateDeployer` (`virtualization-deployment`) | Gets a certificate's material onto a `DeploymentTarget` (`VpsDeploymentTarget` today: a directory + a `ReverseProxy` to reload) and triggers a reload. Pulls material from its own `CertificateStore` — no `CertificateMaterial` parameter, so key material never has to pass through a caller that only wants to trigger a deployment. `LocalFilesystemCertificateDeployer` writes `cert.pem`/`privkey.pem`/`chain.pem` atomically (temp-write, then `ATOMIC_MOVE`; any existing file is kept as `.previous`, never deleted, so a failure before the move leaves the previously-deployed files untouched) and only *logs* a reload — not a real SSH-based remote deployer yet. |

### CLI

See [Commands](#commands) above for the full command reference and the CLI's own state/passphrase
persistence notes.

### REST API

Exposed by `virtualization-spring-boot-autoconfigure` when `spring-boot-starter-web` is present and
gated independently per subsystem (see [Spring Boot integration](#spring-boot-integration)):

| Method & path | Gate | |
|---|---|---|
| `GET /api/v1/domains` | `virtualization.domains.enabled=true` | list |
| `GET /api/v1/domains/{name}` | | get |
| `GET /api/v1/domains/{name}/records` | | list DNS records |
| `POST /api/v1/domains/{name}/records` | | create a DNS record, body `{"name","type","value","ttl","priority"}` |
| `DELETE /api/v1/domains/{name}/records/{id}` | | delete |
| `GET /api/v1/certificates` | `virtualization.certificates.enabled=true` | list |
| `GET /api/v1/certificates/{id}` | | get |
| `POST /api/v1/certificates` | | request, body `{"domains","issuer","challenge"}`, returns metadata only, never key material |
| `POST /api/v1/certificates/{id}/renew` | | renew — `409` if the certificate is `REVOKED`/`FAILED` |
| `POST /api/v1/certificates/{id}/revoke` | | revoke |

There's no `POST /api/v1/domains` (domain registration) — see
[Known limitations](#known-limitations).

## Extension guide

**Adding a new provider module** (e.g. `virtualization-vsphere`):

1. Depend only on `virtualization-core` (never Spring, never another provider).
2. Define a `<Provider>Credentials` type for whatever the backend's auth actually needs — do not
   reuse another provider's credential shape.
3. Implement `VirtualizationProvider`, mapping the backend's own DTOs to `virtualization-core`
   domain records in an `internal` package — never expose a provider DTO through the public API.
4. Drive lifecycle operations through `OperationHandle`, matching whatever async mechanism the
   backend actually offers (task polling like Proxmox, long-poll like Incus, or protocol events
   like QEMU) — don't force one pattern where it doesn't fit.
5. Write tests against an in-process fake server (see `FakeProxmoxServer`/`FakeIncusServer`/
   `FakeQmpServer` for the pattern), never a real backend.
6. Wire it into the CLI's `ProviderFactory` and the Spring `ProviderFactory`/`*ProviderProperties`
   the same way the three existing providers are.

**Exposing more of a provider's REST surface** (LXC containers, snapshots, storage, networks,
Incus profiles/projects): `VirtualizationProvider` deliberately doesn't have methods for these
yet. the spec calls for a *small* public API, and every one of these needs its own capability flag
and CLI/Spring wiring. Add the method to `VirtualizationProvider` (with a matching `Capability`
and default `UnsupportedCapabilityException` for providers that don't implement it), then
implement it per provider.

**Adding a real `DnsProvider`** (Cloudflare, Route53, ...): depend only on `virtualization-dns`
(never `virtualization-domain`/`-certificate`, which depend on it, not the other way round).
Implement `zones()`/`getZone()`/`records()`/`createRecord()`/`updateRecord()`/`deleteRecord()`
against the backend's real API; zone creation stays outside this SDK's `DnsProvider` interface, the
same stance `InMemoryDnsProvider.addZone` already takes. Wire a new `type:` value into
`DnsAutoConfiguration`/`DnsProperties` (Spring) and `DnsConfigLoader` (CLI) alongside the existing
`type: mock`.

**Adding a real `AcmeProvider`** (a real Let's Encrypt/ZeroSSL/internal-CA client): depend only on
`virtualization-certificate` (+ `virtualization-dns` if it's DNS-01-based, like
`virtualization-acme-mock`). `request` stays async (real issuance waits on real challenge
validation); `renew`/`revoke` take the `Certificate` the caller already has, not a bare id. Wire a
new `type:` value into `CertificateAutoConfiguration`/`CertificateProperties` and
`CertificateConfigLoader`.

**Adding a real (SSH-based) `CertificateDeployer`**: depend only on `virtualization-deployment`.
Pull material from the `CertificateStore` you're constructed with (never accept it as a `deploy`
parameter — key material shouldn't have to pass through an unrelated caller). Keep the "known,
fixed set of operations" contract `LocalFilesystemCertificateDeployer` already has — write specific
files, run a specific, restricted reload command — never an arbitrary-command API.

**Adding `ImageProvider` to an existing provider module** (Proxmox, QEMU, or a future one): follow
the same shape `IncusImageProvider` already established — map the backend's own image
representation to `Image` in an `internal` mapper, declare only the `ImageCapability`s actually
implemented, and default the rest of the interface's `pull`/`download`/`importImage` (they already
throw `UnsupportedCapabilityException` unless overridden). Wire it into `ConfigLoader`/
`ProviderFactory` (CLI) and `ProviderFactory` (Spring) via `createImageProvider(name, entry)` —
`Optional.empty()` for provider types that still have none, exactly like Proxmox/QEMU do today.

## Testing

```
./mvnw clean verify
```

runs the full suite — 475+ tests, no external services:

- **Core**: domain model validation, provider registry, capability enforcement, `Operation`
  lifecycle (progress bounds, completion, failure, timeout), plus the image-management model
  (`Image`/`ImageReference`/`ImageQuery`/`ImageCapabilities`, `ImagePullOperation`/
  `ImageImportOperation`/`CreateWorkloadOperation` composing the same `Operation` machinery,
  `ImageSource`, `ImageDownload`, `WorkloadSpec`, default-`UnsupportedCapabilityException`
  behavior for both `ImageProvider` and `VirtualizationProvider.createFromImage`).
- **Proxmox / Incus**: HTTP transport, JSON envelope parsing, HTTP-status → exception mapping,
  VM lifecycle (success and failure), against an in-process fake server built on the JDK's own
  `com.sun.net.httpserver.HttpServer` (no test-only HTTP dependency). Incus additionally covers
  image list/inspect/aliases/search, streamed pull/download/import (including upload byte-count
  progress and download-side no-full-buffering), `createFromImage` under all three
  `ImageAvailabilityPolicy` values, and VPS-provisioning-related cloud-init synthesis (including a
  YAML-injection defense test) and `devices` overrides (root disk, nic).
- **VPS**: `DefaultVpsManager`'s full state machine (every legal/illegal transition, idempotency,
  concurrent `create()` under a `CountDownLatch`-synchronized fixed thread pool sized to the party
  count, readiness-check retry/failure), `DefaultVpsProvisioner` composing fakes of both
  `VirtualizationProvider` and `ImageProvider`, and `TcpReadinessChecker` against a real loopback
  `ServerSocket`.
- **QEMU**: greeting/handshake, request-id correlation, error responses, command timeout,
  asynchronous events, reconnect — against a fake QMP server over TCP, plus a raw AF_UNIX socket
  smoke test for the Unix transport.
- **DNS / Domain**: `InMemoryDnsProvider`'s zone/record CRUD; `DefaultDomainManager`'s
  register/normalize/associate-provider flow and label-walking zone resolution (exact-match apex
  and subdomain cases both).
- **Certificate / ACME**: `DefaultCertificateManager`'s full status lifecycle including the
  renew/revoke-blocked-on-`REVOKED`/`FAILED` guard; `Dns01AcmeProvider`'s real DNS-01 workflow
  (challenge record creation, cleanup always running via `try`/`finally` even on failure, a repeated
  full-reactor run that specifically guards against a once-real race between `succeed()` and
  cleanup completing); `CertificateRenewalScheduler`'s due/not-due/blocked-status/partial-failure
  behavior.
- **Provisioning / Deployment**: `VpsProvisioningService` across all four profiles (including the
  no-static-IP-skips-DNS case and the `CREATE`-vs-`CREATE_AND_UPDATE` policy difference);
  `LocalFilesystemCertificateDeployer`'s atomic file rotation (a failure mid-write leaves the
  previously-deployed files untouched, verified by forcing a failure between steps).
- **CLI**: full command-tree execution via Picocli's `CommandLine` against a fake provider —
  output formatting per `--output`, exit codes, `--provider`/`--output` position independence,
  config file parsing and validation, plus `image`/`workload` command coverage (list, search,
  get, pull, download to a temp file, import, workload create), `vps` command coverage
  (create/list/get/lifecycle/rebuild), and `domain`/`dns`/`certificate` command coverage (DNS
  records and certificates surviving *separate* `execute()` calls — the CLI-persistence property
  that matters, not just in-JVM object reuse; `certificate renew`/`revoke` specifically proven to
  succeed on a certificate requested in an *earlier* invocation, the regression the
  `AcmeProvider.renew`/`.revoke` id-to-`Certificate` signature change fixed; the private-key export
  gate refusing without both `--include-private-key` and `--yes`).
- **Spring**: `ApplicationContextRunner`-based auto-configuration tests — property binding,
  multi-provider wiring for both `ProviderRegistry` and `ImageProviderRegistry`,
  `@ConditionalOnMissingBean` overrides, understandable startup failure on missing configuration,
  `VpsController`'s `@ConditionalOnBean`/`@ConditionalOnProperty` gating (and the equivalent for
  `DomainController`/`CertificateController` — each independently off unless its own
  `virtualization.{domains,certificates}.enabled` flag is set), plus a full `@SpringBootTest`
  context-load test in the example app (now exercising the VM, image, and VPS
  `CommandLineRunner`s at real startup, the last one gracefully reporting "not configured" since
  `VpsService` injects `Optional<VpsManager>`). The REST layer is tested twice: controller unit
  tests via `MockHttpServletRequest`/`Response`, and a `@SpringBootTest` firing real HTTP requests
  at a live embedded Tomcat (including `/api/v1/vps` create/get/409-on-invalid-transition and
  `/api/v1/domains`/`/api/v1/certificates` end to end) — the latter is what caught a routing bug (a
  multi-segment `{id}` needs Spring's `{*id}` capture syntax, not `{id:.+}`) and a binding bug
  (`-parameters` javac flag) that the unit tests alone did not.

Real-backend integration tests are out of scope for this SDK's default build, by design (section
19 of the original spec) — every provider module's tests exercise the real wire protocol, just
against a fake implementation of the other end.

## Known limitations

- `VirtualizationProvider` covers VM/lifecycle listing only. LXC containers (Proxmox), Incus
  containers/images/profiles/networks/storage/projects, and snapshots across all providers are
  reachable via each backend's REST API and already mapped in `virtualization-core`'s domain
  model, but have no interface method yet (see [Extension guide](#extension-guide)).
- QEMU: `destroy` is unsupported (no such QMP concept); configured VM memory size isn't reported
  (would need `query-memory-size-summary`, not in the initial command set); reconnecting a
  `QmpClient` concurrently with in-flight commands from another thread isn't guaranteed safe.
  It's meant to be driven by one `QemuProvider` at a time.
- Incus client keys must be PKCS#8; legacy PKCS#1 needs converting first (see above). No
  BouncyCastle dependency was added just to parse the older format.
- `virtualization-spring-security` ships no default `SecurityFilterChain` and no default
  `ClaimsToRoleMapper` both are application-specific by nature.
- **Image management**: only `virtualization-incus` implements `ImageProvider`; Proxmox and QEMU
  have none. `ImageProvider.delete` doesn't exist yet (not on the interface, not in the CLI, not
  in the REST API), nor does `SNAPSHOT`/`PUBLISH` (workload → image). Browsing/searching Incus's
  public simplestreams catalogs (`images:`/`ubuntu:`/`ubuntu-daily:`) before a `pull` isn't
  supported — this SDK never speaks simplestreams itself, only the connected Incus server does,
  server-side, during `pull`; you pull by a known alias, you don't discover one through this SDK.
  `download`'s checksum matches only a unified (single-file) Incus image export, not a split
  metadata+rootfs one. The REST API's `pull`/`import`/create-workload endpoints are synchronous
  (bounded 5-minute wait, no polling endpoint) even though the underlying SDK operations are
  async. Incus's `project` scoping (`--project` in the CLI, `providerOptions.project` elsewhere)
  is accepted but not actually threaded through to the Incus API yet — it would need to become a
  query parameter on every request the provider makes, a bigger structural change than what's here
  today. `WorkloadSpec.storage`/`networks`/`mounts`/`ports` are free-form strings, not typed
  structures — nothing has needed more structure than that yet.
- **VPS management**: `VpsManager` is bound to exactly one configured provider
  (`virtualization.vps.provider` in Spring/REST, `--provider` in the CLI), not multi-provider
  routed like the image/workload endpoints — `Vps.provider()` stores the provider *type* (e.g.
  `"incus"`), not a registry key, so there's no way back to a specific registry entry for an
  existing VPS; genuine multi-provider VPS routing needs a bigger structural change than this
  layer has today. `volumeType`/`location`/`project` providerOptions are accepted by
  `DefaultVpsProvisioner` but `IncusProvider` doesn't apply any of them yet (see
  [Incus provisioning](#incus-provisioning)). `TcpReadinessChecker` isn't wired into any default
  `VpsManager` construction (Spring, CLI) — `alwaysReady()` stays the default; opt in explicitly
  if you want a real reachability check before `READY`. The CLI's `JsonFileVpsRepository` does a
  whole-file read-modify-write under an in-process lock only, no OS-level file locking — fine for
  one user driving the CLI sequentially, not for concurrent CLI processes racing the same file.
  `VpsRepository` has exactly two implementations: in-memory (used by the Spring/REST wiring — the
  application process stays alive, so that's sufficient) and the CLI's JSON file; no database-backed
  one exists yet.
- **Domain, DNS & certificate management**: only mock implementations exist —
  `InMemoryDnsProvider` (no real Cloudflare/Route53/...) and `Dns01AcmeProvider` (no real Let's
  Encrypt/ZeroSSL/... wire client); both are genuine, tested implementations of their interfaces,
  not test doubles, they just never talk to a real external service. Nothing anywhere (CLI, REST,
  or otherwise) calls `DomainManager.register` — `domain list`/`domain get` and `GET
  /api/v1/domains` are always empty/`404` unless a caller registers a domain itself, in Java, via
  the `DomainManager` bean/instance (see `VirtualizationWebIntegrationTest.TestApp` for the
  pattern); there is deliberately no `POST /api/v1/domains` or `domain register` CLI command yet.
  `CertificateDeployer` has one implementation, `LocalFilesystemCertificateDeployer` — local
  filesystem only, its reload step only logs, no real SSH-based remote deployer exists.
  `VpsProvisioningService` (VPS + DNS + certificate orchestration) is a library API only — it has
  no CLI command and no Spring bean/REST endpoint of its own yet, a caller wires it up
  programmatically from the three managers it composes. `CertificateRenewalScheduler` doesn't
  schedule itself either — nothing calls it on a timer by default (no `@Scheduled` bean, no CLI cron
  command); a caller decides when `renewDue()` runs. The CLI's certificate material store
  (`certificates.enc`) uses one passphrase for the whole file, derived fresh per read/write (PBKDF2,
  210,000 iterations) — there's no per-certificate key, and rotating the passphrase means
  decrypting with the old one and re-encrypting with the new one yourself, no dedicated command for
  it. `DnsRecordType` covers `A`/`AAAA`/`CNAME`/`TXT`/`MX` only — no `NS`/`SRV`. `ChallengeType`
  names `HTTP_01` but no provider implements it.
