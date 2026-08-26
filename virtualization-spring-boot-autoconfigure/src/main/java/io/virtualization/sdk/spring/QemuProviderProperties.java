package io.virtualization.sdk.spring;

import io.virtualization.sdk.core.exception.ConfigurationException;

import static io.virtualization.sdk.spring.internal.RequiredField.require;

/**
 * Validated view of a {@code type: qemu} {@link ProviderProperties} entry — exactly one of
 * {@link #socket()} (a Unix domain socket path) or {@link #host()}/{@link #port()} (a TCP
 * endpoint) is set.
 */
public record QemuProviderProperties(String socket, String host, Integer port) {

    static QemuProviderProperties from(String providerName, ProviderProperties properties) {
        if (properties.socket() != null && !properties.socket().isBlank()) {
            return new QemuProviderProperties(properties.socket(), null, null);
        }
        String host = require(providerName, "qemu", properties.host(), "host");
        if (properties.port() == null) {
            throw new ConfigurationException("Provider '" + providerName + "' has type 'qemu' but 'port' is missing.");
        }
        return new QemuProviderProperties(null, host, properties.port());
    }
}
