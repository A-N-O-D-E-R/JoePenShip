package io.virtualization.sdk.vps;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Lifecycle state of a {@link Vps}. Owns structural transition validation ({@link
 * #canTransitionTo}) — whether a given edge is ever legal. {@link DefaultVpsManager} additionally
 * gates which *source* states make sense for a given operation (e.g. {@code start} vs {@code
 * restart} both target {@link #STARTING} but from disjoint legal sources) — that's a narrower,
 * operation-specific rule this enum doesn't (and shouldn't) know about.
 */
public enum VpsState {
    PROVISIONING,
    STOPPED,
    STARTING,
    RUNNING,
    READY,
    STOPPING,
    REBUILDING,
    ERROR,
    DESTROYING,
    DESTROYED;

    private static final Map<VpsState, Set<VpsState>> TRANSITIONS = Map.ofEntries(
            Map.entry(PROVISIONING, EnumSet.of(READY, ERROR)),
            Map.entry(READY, EnumSet.of(STOPPING, DESTROYING, REBUILDING, ERROR)),
            Map.entry(RUNNING, EnumSet.of(STARTING, STOPPING, DESTROYING, ERROR)),
            Map.entry(STOPPED, EnumSet.of(STARTING, DESTROYING, REBUILDING, ERROR)),
            Map.entry(STARTING, EnumSet.of(RUNNING, ERROR)),
            Map.entry(STOPPING, EnumSet.of(STOPPED, ERROR)),
            Map.entry(REBUILDING, EnumSet.of(READY, ERROR)),
            Map.entry(ERROR, EnumSet.of(DESTROYING)),
            Map.entry(DESTROYING, EnumSet.of(DESTROYED, ERROR)),
            Map.entry(DESTROYED, EnumSet.noneOf(VpsState.class)));

    public boolean canTransitionTo(VpsState target) {
        return TRANSITIONS.get(this).contains(target);
    }
}
