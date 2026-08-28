package io.virtualization.sdk.iam;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * PBKDF2WithHmacSHA256, 210,000 iterations (current OWASP baseline) — the same parameters {@code
 * EncryptedFileCertificateStore} (in {@code virtualization-cli}) uses for its own key derivation,
 * reused here for the identical reason: a slow KDF is the right tool against a low-entropy
 * human-chosen secret (see {@link ApiTokens} for why a fast digest is correct for a high-entropy
 * random token instead). Encoded format is self-describing ({@code
 * pbkdf2-sha256$<iterations>$<salt-b64>$<hash-b64>}) so a future iteration-count bump doesn't
 * invalidate already-stored hashes.
 */
public final class PasswordHasher {

    private static final String ALGORITHM_LABEL = "pbkdf2-sha256";
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {}

    public static String hash(char[] password) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return ALGORITHM_LABEL + "$" + ITERATIONS + "$" + encode(salt) + "$" + encode(hash);
    }

    public static boolean verify(char[] password, String encoded) {
        String[] parts = encoded.split("\\$");
        if (parts.length != 4 || !ALGORITHM_LABEL.equals(parts[0])) {
            return false;
        }
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = derive(password, salt, iterations);
        return MessageDigest.isEqual(expected, actual);
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 unavailable", e);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private static String encode(byte[] bytes) {
        return Base64.getEncoder().withoutPadding().encodeToString(bytes);
    }
}
