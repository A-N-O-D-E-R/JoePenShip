package io.sablier.core;

/**
 * Checks whether a workload is actually ready to serve traffic — a running container/VM is not
 * necessarily a <em>ready</em> one. Pure Java (TCP/HTTP implementations use only {@code
 * java.net}/{@code java.net.http}), so this lives in {@code sablier-core}, not any provider
 * module — a provider adapter selects which checker applies to a given workload (see {@link
 * ReadinessCheckers#fromSpec}), it does not reimplement the check itself.
 */
public interface ReadinessChecker {

    ReadinessStatus check(Workload workload);
}
