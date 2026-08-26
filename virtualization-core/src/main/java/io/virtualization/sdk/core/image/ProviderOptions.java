package io.virtualization.sdk.core.image;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provider-specific {@link WorkloadSpec} extensions the common model can't represent (e.g. Incus
 * {@code "profile"}/{@code "project"}). Deliberately loose — unlike the rest of {@link
 * WorkloadSpec}, this is where provider-specific escape-hatch values live, separated from the
 * strongly-typed common fields rather than replacing them.
 */
public final class ProviderOptions {

    private static final ProviderOptions EMPTY = new ProviderOptions(Map.of());

    private final Map<String, Object> values;

    private ProviderOptions(Map<String, Object> values) {
        this.values = values;
    }

    public static ProviderOptions empty() {
        return EMPTY;
    }

    public static ProviderOptions of(Map<String, Object> values) {
        Objects.requireNonNull(values, "values must not be null");
        return new ProviderOptions(Map.copyOf(values));
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(values.get(key));
    }

    public Optional<String> getString(String key) {
        return get(key).map(Object::toString);
    }

    public Map<String, Object> asMap() {
        return values;
    }
}
