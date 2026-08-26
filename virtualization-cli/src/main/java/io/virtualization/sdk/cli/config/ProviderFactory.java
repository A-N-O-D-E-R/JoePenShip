package io.virtualization.sdk.cli.config;

import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.incus.IncusClientConfig;
import io.virtualization.sdk.incus.IncusImageProvider;
import io.virtualization.sdk.incus.IncusProvider;
import io.virtualization.sdk.incus.IncusTlsCredentials;
import io.virtualization.sdk.proxmox.ProxmoxClientConfig;
import io.virtualization.sdk.proxmox.ProxmoxCredentials;
import io.virtualization.sdk.proxmox.ProxmoxProvider;
import io.virtualization.sdk.qemu.QemuClientConfig;
import io.virtualization.sdk.qemu.QemuProvider;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/** Builds a {@link VirtualizationProvider} from one {@link ProviderEntry}, validating required fields per type. */
public final class ProviderFactory {

    private ProviderFactory() {}

    public static VirtualizationProvider create(String name, ProviderEntry entry) {
        if (entry.type() == null || entry.type().isBlank()) {
            throw new ConfigurationException("Provider '" + name + "' is missing required field 'type'.");
        }
        String type = entry.type();
        return switch (type) {
            case "proxmox" -> createProxmox(name, entry);
            case "incus" -> createIncus(name, entry);
            case "qemu" -> createQemu(name, entry);
            default -> throw new ConfigurationException("Provider '" + name + "' has unknown type '" + type + "'");
        };
    }

    /**
     * Builds the {@link ImageProvider} counterpart of {@link #create}, if the provider's type has
     * one — currently only {@code incus}. Proxmox and QEMU don't implement image management yet.
     */
    public static Optional<ImageProvider> createImageProvider(String name, ProviderEntry entry) {
        if (!"incus".equals(entry.type())) {
            return Optional.empty();
        }
        return Optional.of(new IncusImageProvider(buildIncusConfig(name, entry)));
    }

    private static VirtualizationProvider createProxmox(String name, ProviderEntry entry) {
        String endpoint = require(name, "proxmox", entry.endpoint(), "endpoint");
        String tokenId = require(name, "proxmox", entry.tokenId(), "token-id");
        String tokenSecret = require(name, "proxmox", entry.tokenSecret(), "token-secret");
        boolean verifySsl = entry.verifySsl() == null || entry.verifySsl();

        ProxmoxCredentials credentials = new ProxmoxCredentials(tokenId, tokenSecret);
        ProxmoxClientConfig config = new ProxmoxClientConfig(
                URI.create(endpoint), credentials, verifySsl, Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofSeconds(1));
        return new ProxmoxProvider(config);
    }

    private static VirtualizationProvider createIncus(String name, ProviderEntry entry) {
        return new IncusProvider(buildIncusConfig(name, entry));
    }

    private static IncusClientConfig buildIncusConfig(String name, ProviderEntry entry) {
        String endpoint = require(name, "incus", entry.endpoint(), "endpoint");
        String certPath = require(name, "incus", entry.clientCertPath(), "client-cert-path");
        String keyPath = require(name, "incus", entry.clientKeyPath(), "client-key-path");
        boolean verifySsl = entry.verifySsl() == null || entry.verifySsl();

        IncusTlsCredentials credentials = new IncusTlsCredentials(readFile(name, certPath), readFile(name, keyPath));
        return new IncusClientConfig(
                URI.create(endpoint), credentials, "default", verifySsl, Duration.ofSeconds(10), Duration.ofSeconds(30),
                Duration.ofSeconds(5));
    }

    private static VirtualizationProvider createQemu(String name, ProviderEntry entry) {
        if (entry.socket() != null) {
            return new QemuProvider(QemuClientConfig.unixSocket(name, Path.of(entry.socket())));
        }
        String host = require(name, "qemu", entry.host(), "host");
        if (entry.port() == null) {
            throw new ConfigurationException("Provider '" + name + "' has type 'qemu' but 'port' is missing.");
        }
        return new QemuProvider(QemuClientConfig.tcp(name, host, entry.port()));
    }

    private static String require(String providerName, String type, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ConfigurationException(
                    "Provider '" + providerName + "' has type '" + type + "' but '" + fieldName + "' is missing.");
        }
        return value;
    }

    private static String readFile(String providerName, String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new ConfigurationException("Provider '" + providerName + "' failed to read file '" + path + "'", e);
        }
    }
}
