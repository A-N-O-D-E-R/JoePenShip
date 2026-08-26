package io.virtualization.sdk.vps.support;

import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsReadinessChecker;

import java.util.concurrent.atomic.AtomicInteger;

/** Hand-written test double for {@link VpsReadinessChecker}. Counts calls; never does real I/O. */
public final class FakeVpsReadinessChecker implements VpsReadinessChecker {

    private volatile int readyOnAttempt;
    private final AtomicInteger calls = new AtomicInteger();

    private FakeVpsReadinessChecker(int readyOnAttempt) {
        this.readyOnAttempt = readyOnAttempt;
    }

    public static FakeVpsReadinessChecker readyOnAttempt(int attempt) {
        return new FakeVpsReadinessChecker(attempt);
    }

    public static FakeVpsReadinessChecker neverReady() {
        return new FakeVpsReadinessChecker(Integer.MAX_VALUE);
    }

    /** Flips an already-ready checker to never-ready from this point on, resetting the call count. */
    public void neverReadyFromNowOn() {
        readyOnAttempt = Integer.MAX_VALUE;
        calls.set(0);
    }

    @Override
    public boolean isReady(Vps vps) {
        return calls.incrementAndGet() >= readyOnAttempt;
    }

    public int callCount() {
        return calls.get();
    }
}
