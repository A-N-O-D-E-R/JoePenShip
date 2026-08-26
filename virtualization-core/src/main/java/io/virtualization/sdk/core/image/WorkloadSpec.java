package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.ComputeResources;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provider-neutral specification for creating a workload (container or VM), typically from an
 * image via {@link io.virtualization.sdk.core.VirtualizationProvider#createFromImage}.
 *
 * <p>{@code storage}/{@code networks}/{@code mounts}/{@code ports} are free-form provider-
 * interpreted specifiers (e.g. {@code "root:20GB"}, {@code "8080:80/tcp"}) rather than typed
 * structures — introduce dedicated types for these once a provider actually needs more structure
 * than a string. Anything else provider-specific belongs in {@link #providerOptions()}.
 */
public final class WorkloadSpec {

    private final String name;
    private final WorkloadType type;
    private final ImageReference image;
    private final String architecture;
    private final ComputeResources resources;
    private final List<String> storage;
    private final List<String> networks;
    private final Map<String, String> environment;
    private final List<String> mounts;
    private final List<String> ports;
    private final Map<String, String> metadata;
    private final Map<String, String> labels;
    private final Map<String, String> configuration;
    private final ProviderOptions providerOptions;

    private WorkloadSpec(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.type = Objects.requireNonNull(builder.type, "type must not be null");
        this.image = builder.image;
        this.architecture = builder.architecture;
        this.resources = builder.resources;
        this.storage = List.copyOf(builder.storage);
        this.networks = List.copyOf(builder.networks);
        this.environment = Map.copyOf(builder.environment);
        this.mounts = List.copyOf(builder.mounts);
        this.ports = List.copyOf(builder.ports);
        this.metadata = Map.copyOf(builder.metadata);
        this.labels = Map.copyOf(builder.labels);
        this.configuration = Map.copyOf(builder.configuration);
        this.providerOptions = builder.providerOptions;
    }

    public String name() {
        return name;
    }

    public WorkloadType type() {
        return type;
    }

    public Optional<ImageReference> image() {
        return Optional.ofNullable(image);
    }

    public Optional<String> architecture() {
        return Optional.ofNullable(architecture);
    }

    public Optional<ComputeResources> resources() {
        return Optional.ofNullable(resources);
    }

    public List<String> storage() {
        return storage;
    }

    public List<String> networks() {
        return networks;
    }

    public Map<String, String> environment() {
        return environment;
    }

    public List<String> mounts() {
        return mounts;
    }

    public List<String> ports() {
        return ports;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public Map<String, String> labels() {
        return labels;
    }

    public Map<String, String> configuration() {
        return configuration;
    }

    public ProviderOptions providerOptions() {
        return providerOptions;
    }

    public static Builder builder(String name, WorkloadType type) {
        return new Builder(name, type);
    }

    public static final class Builder {

        private final String name;
        private final WorkloadType type;
        private ImageReference image;
        private String architecture;
        private ComputeResources resources;
        private List<String> storage = List.of();
        private List<String> networks = List.of();
        private Map<String, String> environment = Map.of();
        private List<String> mounts = List.of();
        private List<String> ports = List.of();
        private Map<String, String> metadata = Map.of();
        private Map<String, String> labels = Map.of();
        private Map<String, String> configuration = Map.of();
        private ProviderOptions providerOptions = ProviderOptions.empty();

        private Builder(String name, WorkloadType type) {
            this.name = name;
            this.type = type;
        }

        public Builder image(ImageReference image) {
            this.image = image;
            return this;
        }

        public Builder architecture(String architecture) {
            this.architecture = architecture;
            return this;
        }

        public Builder resources(ComputeResources resources) {
            this.resources = resources;
            return this;
        }

        public Builder storage(List<String> storage) {
            this.storage = storage;
            return this;
        }

        public Builder networks(List<String> networks) {
            this.networks = networks;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public Builder mounts(List<String> mounts) {
            this.mounts = mounts;
            return this;
        }

        public Builder ports(List<String> ports) {
            this.ports = ports;
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

        public Builder configuration(Map<String, String> configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder providerOptions(ProviderOptions providerOptions) {
            this.providerOptions = providerOptions;
            return this;
        }

        public WorkloadSpec build() {
            return new WorkloadSpec(this);
        }
    }
}
