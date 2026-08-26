package io.virtualization.sdk.incus.internal;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Incus {@code limits.memory} config values (e.g. {@code "2GiB"}, {@code "512MiB"},
 * {@code "1073741824"} bytes) into megabytes.
 */
public final class MemorySizeParser {

    private static final Pattern SIZE_PATTERN = Pattern.compile("(?i)^(\\d+)\\s*(kib|mib|gib|tib|kb|mb|gb|tb|b)?$");

    private MemorySizeParser() {}

    /**
     * @param value  an Incus size string, or {@code null}
     * @param defaultMb value to return when {@code value} is null, blank, or unparseable
     */
    public static long toMegabytes(String value, long defaultMb) {
        if (value == null || value.isBlank()) {
            return defaultMb;
        }
        Matcher matcher = SIZE_PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            return defaultMb;
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2) == null ? "b" : matcher.group(2).toLowerCase();
        long bytes = switch (unit) {
            case "kib" -> amount * 1024L;
            case "mib" -> amount * 1024L * 1024L;
            case "gib" -> amount * 1024L * 1024L * 1024L;
            case "tib" -> amount * 1024L * 1024L * 1024L * 1024L;
            case "kb" -> amount * 1000L;
            case "mb" -> amount * 1000L * 1000L;
            case "gb" -> amount * 1000L * 1000L * 1000L;
            case "tb" -> amount * 1000L * 1000L * 1000L * 1000L;
            default -> amount;
        };
        return bytes / (1024L * 1024L);
    }
}
