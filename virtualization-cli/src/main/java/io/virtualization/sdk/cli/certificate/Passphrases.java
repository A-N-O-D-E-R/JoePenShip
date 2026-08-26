package io.virtualization.sdk.cli.certificate;

import io.virtualization.sdk.core.exception.ConfigurationException;

import java.util.function.Supplier;

/**
 * Resolves the passphrase used to encrypt/decrypt {@link EncryptedFileCertificateStore}'s file. A
 * CLI {@code --passphrase} flag takes precedence but {@link #ENV_VAR} is preferred, since a flag
 * value may land in shell history.
 */
public final class Passphrases {

    public static final String ENV_VAR = "VIRTUALIZATION_CERTIFICATE_PASSPHRASE";

    private Passphrases() {}

    public static Supplier<char[]> resolve(String cliValue) {
        return () -> {
            if (cliValue != null && !cliValue.isBlank()) {
                return cliValue.toCharArray();
            }
            String envValue = System.getenv(ENV_VAR);
            if (envValue != null && !envValue.isBlank()) {
                return envValue.toCharArray();
            }
            throw new ConfigurationException("No certificate passphrase available — pass --passphrase or set " + ENV_VAR + ".");
        };
    }

    /** For commands that provably never touch {@link EncryptedFileCertificateStore#store}/{@code load} — throws only if that assumption is ever violated. */
    public static Supplier<char[]> unavailable() {
        return () -> {
            throw new IllegalStateException("Certificate material store accessed without a passphrase supplier");
        };
    }
}
