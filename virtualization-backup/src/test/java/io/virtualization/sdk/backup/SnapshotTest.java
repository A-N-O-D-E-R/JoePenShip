package io.virtualization.sdk.backup;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SnapshotTest {

    @Test
    void requiresInstanceId() {
        assertThatNullPointerException().isThrownBy(() -> new Snapshot(null, "before-upgrade", false, Instant.now()));
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Snapshot("vm-1", "", false, Instant.now()));
    }

    @Test
    void requiresCreatedAt() {
        assertThatNullPointerException().isThrownBy(() -> new Snapshot("vm-1", "before-upgrade", false, null));
    }
}
