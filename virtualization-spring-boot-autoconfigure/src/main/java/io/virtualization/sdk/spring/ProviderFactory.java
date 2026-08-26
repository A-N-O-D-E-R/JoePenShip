package io.virtualization.sdk.spring;

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

/** Builds a {@link VirtualizationProvider} from one {@link ProviderProperties} entry, validating required fields per type. */
final class ProviderFactory {

    private ProviderFactory() {}

    static VirtualizationProvider create(String name, ProviderProperties properties) {
        if (properties.type() == null || properties.type().isBlank()) {
            throw new ConfigurationException("Provider '" + name + "' is missing required field 'type'.");
        }
        return switch (properties.type()) {
            case "proxmox" -> createProxmox(name, properties);
            case "incus" -> createIncus(name, properties);
            case "qemu" -> createQemu(name, properties);
            default -> throw new ConfigurationException("Provider '" + name + "' has unknown type '" + properties.type() + "'");
        };
    }

    /**
     * Builds the {@link ImageProvider} counterpart of {@link #create}, if the provider's type has
     * one — currently only {@code incus}. Proxmox and QEMU don't implement image management yet.
     */
    static Optional<ImageProvider> createImageProvider(String name, ProviderProperties properties) {
        if (!"incus".equals(properties.type())) {
            return Optional.empty();
        }
        return Optional.of(new IncusImageProvider(buildIncusConfig(name, properties)));
    }

    private static VirtualizationProvider createProxmox(String name, ProviderProperties properties) {
        ProxmoxProviderProperties typed = ProxmoxProviderProperties.from(name, properties);
        ProxmoxCredentials credentials = new ProxmoxCredentials(typed.tokenId(), typed.tokenSecret());
        ProxmoxClientConfig config = new ProxmoxClientConfig(
                URI.create(typed.endpoint()),
                credentials,
                typed.verifySsl(),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(1));
        return new ProxmoxProvider(config);
    }

    private static VirtualizationProvider createIncus(String name, ProviderProperties properties) {
        return new IncusProvider(buildIncusConfig(name, properties));
    }

    private static IncusClientConfig buildIncusConfig(String name, ProviderProperties properties) {
        IncusProviderProperties typed = IncusProviderProperties.from(name, properties);
        IncusTlsCredentials credentials =
                new IncusTlsCredentials(readFile(name, typed.clientCertPath()), readFile(name, typed.clientKeyPath()));
        return new IncusClientConfig(
                URI.create(typed.endpoint()),
                credentials,
                "default",
                typed.verifySsl(),
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(5));
    }

    private static VirtualizationProvider createQemu(String name, ProviderProperties properties) {
        QemuProviderProperties typed = QemuProviderProperties.from(name, properties);
        QemuClientConfig config = typed.socket() != null
                ? QemuClientConfig.unixSocket(name, Path.of(typed.socket()))
                : QemuClientConfig.tcp(name, typed.host(), typed.port());
        return new QemuProvider(config);
    }

    private static String readFile(String providerName, String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            throw new ConfigurationException("Provider '" + providerName + "' failed to read file '" + path + "'", e);
        }
    }
}
