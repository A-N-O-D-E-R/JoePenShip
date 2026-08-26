package io.virtualization.sdk.vps;

/**
 * Checks whether a freshly (re)provisioned {@link Vps} is actually reachable, not just reported
 * created by the {@link VpsProvisioner}. {@link DefaultVpsManager} calls this before flipping
 * {@code PROVISIONING}/{@code REBUILDING} to {@code READY}.
 */
@FunctionalInterface
public interface VpsReadinessChecker {

    boolean isReady(Vps vps);

    /** Skips the check entirely — the {@link VpsProvisioner}'s own success signal is trusted. */
    static VpsReadinessChecker alwaysReady() {
        return vps -> true;
    }
}
