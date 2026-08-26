package io.virtualization.sdk.core.image;

import java.util.Optional;

/**
 * Filter criteria for {@link ImageProvider#search(ImageQuery)}. Every field is optional — an
 * empty query matches every image. Provider modules may support additional filtering beyond these
 * common fields.
 */
public final class ImageQuery {

    private final String name;
    private final String alias;
    private final String architecture;
    private final String operatingSystem;
    private final String distribution;
    private final String version;
    private final ImageType type;
    private final String remote;
    private final String provider;

    private ImageQuery(Builder builder) {
        this.name = builder.name;
        this.alias = builder.alias;
        this.architecture = builder.architecture;
        this.operatingSystem = builder.operatingSystem;
        this.distribution = builder.distribution;
        this.version = builder.version;
        this.type = builder.type;
        this.remote = builder.remote;
        this.provider = builder.provider;
    }

    public Optional<String> name() {
        return Optional.ofNullable(name);
    }

    public Optional<String> alias() {
        return Optional.ofNullable(alias);
    }

    public Optional<String> architecture() {
        return Optional.ofNullable(architecture);
    }

    public Optional<String> operatingSystem() {
        return Optional.ofNullable(operatingSystem);
    }

    public Optional<String> distribution() {
        return Optional.ofNullable(distribution);
    }

    public Optional<String> version() {
        return Optional.ofNullable(version);
    }

    public Optional<ImageType> type() {
        return Optional.ofNullable(type);
    }

    public Optional<String> remote() {
        return Optional.ofNullable(remote);
    }

    public Optional<String> provider() {
        return Optional.ofNullable(provider);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String name;
        private String alias;
        private String architecture;
        private String operatingSystem;
        private String distribution;
        private String version;
        private ImageType type;
        private String remote;
        private String provider;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder architecture(String architecture) {
            this.architecture = architecture;
            return this;
        }

        public Builder operatingSystem(String operatingSystem) {
            this.operatingSystem = operatingSystem;
            return this;
        }

        public Builder distribution(String distribution) {
            this.distribution = distribution;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder type(ImageType type) {
            this.type = type;
            return this;
        }

        public Builder remote(String remote) {
            this.remote = remote;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public ImageQuery build() {
            return new ImageQuery(this);
        }
    }
}
