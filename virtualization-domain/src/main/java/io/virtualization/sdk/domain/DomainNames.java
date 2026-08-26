package io.virtualization.sdk.domain;

import java.net.IDN;
import java.util.Locale;
import java.util.Objects;

/**
 * Normalizes a caller-supplied domain name into the canonical, ASCII/punycode form {@link
 * Domain#name()} requires. {@link IDN#toASCII} does neither lower-casing nor trailing-dot
 * stripping itself (verified), and silently passes a second trailing dot straight through rather
 * than rejecting it — so both are handled explicitly here, before {@code toASCII} ever runs.
 */
public final class DomainNames {

    private DomainNames() {}

    public static String normalize(String rawName) {
        Objects.requireNonNull(rawName, "rawName must not be null");
        if (rawName.isBlank()) {
            throw new IllegalArgumentException("rawName must not be blank");
        }
        String lower = rawName.toLowerCase(Locale.ROOT);
        String stripped = lower.endsWith(".") ? lower.substring(0, lower.length() - 1) : lower;
        if (stripped.isBlank() || stripped.startsWith(".") || stripped.endsWith(".")) {
            throw new IllegalArgumentException("'" + rawName + "' is not a valid domain name");
        }
        try {
            return IDN.toASCII(stripped, IDN.USE_STD3_ASCII_RULES);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + rawName + "' is not a valid domain name", e);
        }
    }
}
