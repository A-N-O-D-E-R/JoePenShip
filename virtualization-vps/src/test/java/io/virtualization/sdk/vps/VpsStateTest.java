package io.virtualization.sdk.vps;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class VpsStateTest {

    private static final Map<VpsState, Set<VpsState>> LEGAL = Map.ofEntries(
            Map.entry(VpsState.PROVISIONING, EnumSet.of(VpsState.READY, VpsState.ERROR)),
            Map.entry(VpsState.READY, EnumSet.of(VpsState.STOPPING, VpsState.DESTROYING, VpsState.REBUILDING, VpsState.ERROR)),
            Map.entry(VpsState.RUNNING, EnumSet.of(VpsState.STARTING, VpsState.STOPPING, VpsState.DESTROYING, VpsState.ERROR)),
            Map.entry(VpsState.STOPPED, EnumSet.of(VpsState.STARTING, VpsState.DESTROYING, VpsState.REBUILDING, VpsState.ERROR)),
            Map.entry(VpsState.STARTING, EnumSet.of(VpsState.RUNNING, VpsState.ERROR)),
            Map.entry(VpsState.STOPPING, EnumSet.of(VpsState.STOPPED, VpsState.ERROR)),
            Map.entry(VpsState.REBUILDING, EnumSet.of(VpsState.READY, VpsState.ERROR)),
            Map.entry(VpsState.ERROR, EnumSet.of(VpsState.DESTROYING)),
            Map.entry(VpsState.DESTROYING, EnumSet.of(VpsState.DESTROYED, VpsState.ERROR)),
            Map.entry(VpsState.DESTROYED, EnumSet.noneOf(VpsState.class)));

    @Test
    void everyEdgeMatchesTheDesignedTable() {
        for (VpsState from : VpsState.values()) {
            for (VpsState to : VpsState.values()) {
                boolean expected = LEGAL.get(from).contains(to);
                assertThat(from.canTransitionTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(expected);
            }
        }
    }

    @Test
    void destroyedIsTerminal() {
        for (VpsState target : VpsState.values()) {
            assertThat(VpsState.DESTROYED.canTransitionTo(target)).isFalse();
        }
    }
}
