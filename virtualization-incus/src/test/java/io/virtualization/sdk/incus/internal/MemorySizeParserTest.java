package io.virtualization.sdk.incus.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemorySizeParserTest {

    @Test
    void parsesBinarySuffixes() {
        assertThat(MemorySizeParser.toMegabytes("2GiB", 0)).isEqualTo(2048);
        assertThat(MemorySizeParser.toMegabytes("512MiB", 0)).isEqualTo(512);
        assertThat(MemorySizeParser.toMegabytes("1KiB", 0)).isEqualTo(0);
    }

    @Test
    void parsesPlainBytes() {
        assertThat(MemorySizeParser.toMegabytes("1073741824", 0)).isEqualTo(1024);
    }

    @Test
    void returnsDefaultForNullBlankOrUnparseable() {
        assertThat(MemorySizeParser.toMegabytes(null, 256)).isEqualTo(256);
        assertThat(MemorySizeParser.toMegabytes("", 256)).isEqualTo(256);
        assertThat(MemorySizeParser.toMegabytes("not-a-size", 256)).isEqualTo(256);
    }
}
