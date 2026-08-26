package io.virtualization.sdk.incus.internal;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

/** Wraps a stream being uploaded, reporting the cumulative byte count read so far as it's consumed. */
public final class CountingInputStream extends FilterInputStream {

    private final LongConsumer onBytesRead;
    private long total = 0;

    public CountingInputStream(InputStream in, LongConsumer onBytesRead) {
        super(in);
        this.onBytesRead = onBytesRead;
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            onBytesRead.accept(++total);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int n = super.read(b, off, len);
        if (n > 0) {
            total += n;
            onBytesRead.accept(total);
        }
        return n;
    }
}
