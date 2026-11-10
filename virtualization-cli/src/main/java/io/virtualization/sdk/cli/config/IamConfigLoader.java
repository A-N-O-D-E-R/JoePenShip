package io.virtualization.sdk.cli.config;

import java.nio.file.Path;

/** Reads the single {@code virtualization.iam.enabled} flag — default/absent is {@code false}, same opt-in stance as {@code virtualization.domains.enabled}/{@code virtualization.certificates.enabled} on the Spring side. */
public final class IamConfigLoader {

    private IamConfigLoader() {}

    public static boolean isEnabled(Path configPath) {
        return ConfigYaml.section(configPath, "virtualization", "iam", "enabled").asBoolean(false);
    }
}
