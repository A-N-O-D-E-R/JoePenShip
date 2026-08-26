package io.virtualization.sdk.vps;

/** A provider-neutral byte size, for {@link VpsSpec} builder sugar and {@link StorageConfiguration}. */
public record DataSize(long bytes) {

    public DataSize {
        if (bytes < 0) {
            throw new IllegalArgumentException("bytes must not be negative, was " + bytes);
        }
    }

    public static DataSize ofBytes(long bytes) {
        return new DataSize(bytes);
    }

    public static DataSize ofKilobytes(long kilobytes) {
        return new DataSize(kilobytes * 1_024);
    }

    public static DataSize ofMegabytes(long megabytes) {
        return new DataSize(megabytes * 1_024 * 1_024);
    }

    public static DataSize ofGigabytes(long gigabytes) {
        return new DataSize(gigabytes * 1_024 * 1_024 * 1_024);
    }

    public long toMegabytes() {
        return bytes / (1_024 * 1_024);
    }
}
