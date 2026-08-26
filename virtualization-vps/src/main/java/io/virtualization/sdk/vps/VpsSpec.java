package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ImageReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Provider-neutral specification for provisioning a {@link Vps}. */
public final class VpsSpec {

    private final String name;
    private final ImageReference image;
    private final VpsType type;
    private final ComputeResources compute;
    private final StorageConfiguration storage;
    private final NetworkConfiguration network;
    private final List<String> sshPublicKeys;
    private final String cloudInit;
    private final Map<String, String> metadata;
    private final Map<String, String> labels;
    private final String location;
    private final String project;
    private final String idempotencyKey;
    private final List<String> domains;
    private final String dnsProvider;
    private final DnsProvisioningPolicy dnsPolicy;
    private final boolean tlsEnabled;
    private final String tlsCertificateIssuer;

    private VpsSpec(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.image = Objects.requireNonNull(builder.image, "image must not be null");
        this.type = builder.type;
        this.compute = builder.resolveCompute();
        this.storage = builder.resolveStorage();
        this.network = builder.network;
        this.sshPublicKeys = List.copyOf(builder.sshPublicKeys);
        this.cloudInit = builder.cloudInit;
        this.metadata = Map.copyOf(builder.metadata);
        this.labels = Map.copyOf(builder.labels);
        this.location = builder.location;
        this.project = builder.project;
        this.idempotencyKey = builder.idempotencyKey;
        this.domains = List.copyOf(builder.domains);
        this.dnsProvider = builder.dnsProvider;
        this.dnsPolicy = builder.dnsPolicy;
        this.tlsEnabled = builder.tlsEnabled;
        this.tlsCertificateIssuer = builder.tlsCertificateIssuer;
    }

    public String name() {
        return name;
    }

    public ImageReference image() {
        return image;
    }

    public VpsType type() {
        return type;
    }

    public Optional<ComputeResources> compute() {
        return Optional.ofNullable(compute);
    }

    public Optional<StorageConfiguration> storage() {
        return Optional.ofNullable(storage);
    }

    public Optional<NetworkConfiguration> network() {
        return Optional.ofNullable(network);
    }

    public List<String> sshPublicKeys() {
        return sshPublicKeys;
    }

    public Optional<String> cloudInit() {
        return Optional.ofNullable(cloudInit);
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public Optional<String> location() {
        return Optional.ofNullable(location);
    }

    public Optional<String> project() {
        return Optional.ofNullable(project);
    }

    public Optional<String> idempotencyKey() {
        return Optional.ofNullable(idempotencyKey);
    }

    public List<String> domains() {
        return domains;
    }

    public Optional<String> dnsProvider() {
        return Optional.ofNullable(dnsProvider);
    }

    public DnsProvisioningPolicy dnsPolicy() {
        return dnsPolicy;
    }

    public boolean tlsEnabled() {
        return tlsEnabled;
    }

    public Optional<String> tlsCertificateIssuer() {
        return Optional.ofNullable(tlsCertificateIssuer);
    }

    public static Builder builder(String name, ImageReference image) {
        return new Builder(name, image);
    }

    public static final class Builder {

        private final String name;
        private final ImageReference image;
        private VpsType type = VpsType.VIRTUAL_MACHINE;

        // compute: whole-object setter wins over cpu()/memory() sugar if both are used.
        private ComputeResources compute;
        private Integer cpuCores;
        private DataSize memory;

        // storage: whole-object setter wins over disk()/storagePool()/volumeType() sugar.
        private StorageConfiguration storage;
        private DataSize disk;
        private String storagePool;
        private String volumeType;

        private NetworkConfiguration network;
        private List<String> sshPublicKeys = List.of();
        private String cloudInit;
        private Map<String, String> metadata = Map.of();
        private Map<String, String> labels = Map.of();
        private String location;
        private String project;
        private String idempotencyKey;
        private List<String> domains = List.of();
        private String dnsProvider;
        private DnsProvisioningPolicy dnsPolicy = DnsProvisioningPolicy.NONE;
        private boolean tlsEnabled;
        private String tlsCertificateIssuer;

        private Builder(String name, ImageReference image) {
            this.name = name;
            this.image = image;
        }

        public Builder type(VpsType type) {
            this.type = type;
            return this;
        }

        public Builder cpu(int cpuCores) {
            this.cpuCores = cpuCores;
            return this;
        }

        public Builder memory(DataSize memory) {
            this.memory = memory;
            return this;
        }

        public Builder compute(ComputeResources compute) {
            this.compute = compute;
            return this;
        }

        public Builder disk(DataSize disk) {
            this.disk = disk;
            return this;
        }

        public Builder storagePool(String storagePool) {
            this.storagePool = storagePool;
            return this;
        }

        public Builder volumeType(String volumeType) {
            this.volumeType = volumeType;
            return this;
        }

        public Builder storage(StorageConfiguration storage) {
            this.storage = storage;
            return this;
        }

        public Builder network(NetworkConfiguration network) {
            this.network = network;
            return this;
        }

        public Builder sshPublicKeys(List<String> sshPublicKeys) {
            this.sshPublicKeys = sshPublicKeys;
            return this;
        }

        public Builder cloudInit(String cloudInit) {
            this.cloudInit = cloudInit;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder location(String location) {
            this.location = location;
            return this;
        }

        public Builder project(String project) {
            this.project = project;
            return this;
        }

        public Builder idempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
            return this;
        }

        public Builder domains(List<String> domains) {
            this.domains = domains;
            return this;
        }

        public Builder dnsProvider(String dnsProvider) {
            this.dnsProvider = dnsProvider;
            return this;
        }

        public Builder dnsPolicy(DnsProvisioningPolicy dnsPolicy) {
            this.dnsPolicy = dnsPolicy;
            return this;
        }

        public Builder tlsEnabled(boolean tlsEnabled) {
            this.tlsEnabled = tlsEnabled;
            return this;
        }

        public Builder tlsCertificateIssuer(String tlsCertificateIssuer) {
            this.tlsCertificateIssuer = tlsCertificateIssuer;
            return this;
        }

        public VpsSpec build() {
            return new VpsSpec(this);
        }

        private ComputeResources resolveCompute() {
            if (compute != null) {
                return compute;
            }
            if (cpuCores == null && memory == null) {
                return null;
            }
            return new ComputeResources(cpuCores != null ? cpuCores : 1, memory != null ? memory.toMegabytes() : 1_024);
        }

        private StorageConfiguration resolveStorage() {
            if (storage != null) {
                return storage;
            }
            if (disk == null && storagePool == null && volumeType == null) {
                return null;
            }
            return new StorageConfiguration(disk != null ? disk : DataSize.ofGigabytes(10), storagePool, volumeType);
        }
    }
}
