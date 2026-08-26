package io.virtualization.sdk.cli.certificate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.certificate.CertificateMaterial;
import io.virtualization.sdk.certificate.CertificateStore;
import io.virtualization.sdk.core.exception.ConfigurationException;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link CertificateStore} backed by a single AES/GCM-encrypted JSON file — the CLI has no
 * long-lived process or secret-manager integration, so certificate material has to persist across
 * invocations somewhere on disk; per the spec's "do not implement plaintext private-key
 * persistence in production code" instruction, that's encrypted, not a plain JSON file.
 *
 * <p>Key derivation: PBKDF2WithHmacSHA256, 210,000 iterations (current OWASP baseline), a fresh
 * random 16-byte salt on every write. Encryption: AES/GCM/NoPadding, 256-bit key, a fresh random
 * 12-byte IV on every write. The passphrase itself never touches disk — only its derived key does,
 * transiently, in memory.
 *
 * <p>ponytail: whole-file read-decrypt-modify-encrypt-write under an in-process lock only, same
 * tradeoff as {@code JsonFileVpsRepository}/{@code JsonFileDnsProvider}.
 */
public final class EncryptedFileCertificateStore implements CertificateStore {

    private static final int KDF_ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path file;
    private final Supplier<char[]> passphrase;

    public EncryptedFileCertificateStore(Path file, Supplier<char[]> passphrase) {
        this.file = Objects.requireNonNull(file, "file must not be null");
        this.passphrase = Objects.requireNonNull(passphrase, "passphrase must not be null");
    }

    @Override
    public synchronized void store(CertificateId id, CertificateMaterial material) {
        Map<String, CertificateMaterial> all = readAll();
        all.put(id.value(), material);
        writeAll(all);
    }

    @Override
    public synchronized Optional<CertificateMaterial> load(CertificateId id) {
        return Optional.ofNullable(readAll().get(id.value()));
    }

    @Override
    public synchronized void delete(CertificateId id) {
        Map<String, CertificateMaterial> all = readAll();
        all.remove(id.value());
        writeAll(all);
    }

    private Map<String, CertificateMaterial> readAll() {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try {
            Envelope envelope = MAPPER.readValue(file.toFile(), Envelope.class);
            byte[] salt = Base64.getDecoder().decode(envelope.salt());
            byte[] iv = Base64.getDecoder().decode(envelope.iv());
            byte[] ciphertext = Base64.getDecoder().decode(envelope.ciphertext());
            byte[] plaintext = decrypt(ciphertext, deriveKey(salt, envelope.kdfIterations()), iv);
            return new LinkedHashMap<>(MAPPER.readValue(plaintext, new TypeReference<Map<String, CertificateMaterial>>() {}));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read certificate material file '" + file + "'", e);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException("Failed to decrypt certificate material file '" + file + "' — wrong passphrase?", e);
        }
    }

    private void writeAll(Map<String, CertificateMaterial> all) {
        try {
            byte[] salt = randomBytes(SALT_LENGTH_BYTES);
            byte[] iv = randomBytes(IV_LENGTH_BYTES);
            byte[] plaintext = MAPPER.writeValueAsBytes(all);
            byte[] ciphertext = encrypt(plaintext, deriveKey(salt, KDF_ITERATIONS), iv);
            Envelope envelope = new Envelope(
                    1, KDF_ITERATIONS, Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(iv),
                    Base64.getEncoder().encodeToString(ciphertext));
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writeValue(file.toFile(), envelope);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write certificate material file '" + file + "'", e);
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException("Failed to encrypt certificate material file '" + file + "'", e);
        }
    }

    private SecretKeySpec deriveKey(byte[] salt, int iterations) throws GeneralSecurityException {
        char[] pass = passphrase.get();
        try {
            PBEKeySpec spec = new PBEKeySpec(pass, salt, iterations, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } finally {
            Arrays.fill(pass, '\0');
        }
    }

    private static byte[] encrypt(byte[] plaintext, SecretKeySpec key, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(plaintext);
    }

    private static byte[] decrypt(byte[] ciphertext, SecretKeySpec key, byte[] iv) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(ciphertext);
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private record Envelope(int version, int kdfIterations, String salt, String iv, String ciphertext) {}
}
