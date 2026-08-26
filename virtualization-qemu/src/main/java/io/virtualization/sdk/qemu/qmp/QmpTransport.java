package io.virtualization.sdk.qemu.qmp;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;

/**
 * Raw newline-delimited-JSON I/O for a QMP connection. {@link QmpClient} speaks QMP's JSON
 * protocol on top of this; implementations only move bytes.
 */
interface QmpTransport extends Closeable {

    void connect(Duration timeout) throws IOException;

    void writeLine(String json) throws IOException;

    /** Blocks until a full line is available. Returns {@code null} at end of stream. */
    String readLine() throws IOException;
}
