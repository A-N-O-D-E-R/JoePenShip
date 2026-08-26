package io.virtualization.sdk.qemu.qmp;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Runs a blocking connect call with an enforced timeout, shared by both {@link QmpTransport} implementations. */
final class TimeoutSupport {

    private TimeoutSupport() {}

    @FunctionalInterface
    interface IoAction {
        void run() throws IOException;
    }

    static void runWithTimeout(Duration timeout, IoAction action) throws IOException {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Void> future = executor.submit(() -> {
                action.run();
                return null;
            });
            try {
                future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new IOException("Timed out after " + timeout, e);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                if (cause instanceof IOException io) {
                    throw io;
                }
                throw new IOException("Connect failed", cause);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while connecting", e);
            }
        }
    }
}
