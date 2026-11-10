package io.virtualization.sdk.iam;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Random API tokens and their verification hash. Deliberately a fast plain digest (SHA-256), not
 * {@link PasswordHasher}'s slow PBKDF2 — a 256-bit random token is already computationally
 * unguessable regardless of hash speed (unlike a human-chosen password), so a slow KDF here would
 * only add real per-request latency (every authenticated CLI invocation/REST call re-hashes the
 * presented token) for zero additional brute-force resistance.
 */
public final class ApiTokens {

    private static final String PREFIX = "vsdk_";
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private ApiTokens() {}

    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
