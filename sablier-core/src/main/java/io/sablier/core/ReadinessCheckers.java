package io.sablier.core;

import java.net.URI;
import java.time.Duration;

/**
 * Builds a {@link ReadinessChecker} from a workload's {@code readinessCheck} spec string (see
 * {@link WorkloadMetadata#readinessCheck()} — e.g. an Incus {@code user.sablier.readiness-check}
 * config value): {@code http://...}/{@code https://...} for {@link HttpReadinessChecker},
 * {@code host:port} for {@link TcpReadinessChecker}.
 *
 * <p>{@code host:port} parsing splits on the last {@code ':'} — this does not support a bare
 * IPv6 literal host (e.g. {@code ::1:8080} is ambiguous); use a hostname or bracketed form isn't
 * supported in this iteration. Not a concern for the common case (a hostname or IPv4 address).
 */
public final class ReadinessCheckers {

    private ReadinessCheckers() {}

    public static ReadinessChecker stateOnly() {
        return new StateReadinessChecker();
    }

    /**
     * @throws IllegalArgumentException if {@code spec} is neither a valid {@code http(s)://} URL
     *     nor a {@code host:port} pair
     */
    public static ReadinessChecker fromSpec(String spec, Duration timeout) {
        if (spec.startsWith("http://") || spec.startsWith("https://")) {
            return new HttpReadinessChecker(URI.create(spec), timeout);
        }
        int colon = spec.lastIndexOf(':');
        if (colon <= 0 || colon == spec.length() - 1) {
            throw new IllegalArgumentException("Unrecognized readiness check spec (expected 'http(s)://...' or 'host:port'): " + spec);
        }
        String host = spec.substring(0, colon);
        int port;
        try {
            port = Integer.parseInt(spec.substring(colon + 1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unrecognized readiness check spec (expected 'http(s)://...' or 'host:port'): " + spec, e);
        }
        return new TcpReadinessChecker(host, port, timeout);
    }
}
