package io.virtualization.sdk.backup;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class BackupTest {

    @Test
    void requiresInstanceId() {
        assertThatNullPointerException().isThrownBy(() -> new Backup(null, "nightly", Instant.now(), null));
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Backup("vm-1", "", Instant.now(), null));
    }

    @Test
    void expiresAtIsOptional() {
        Backup backup = new Backup("vm-1", "nightly", Instant.now(), null);

        assertThat(backup.expiresAt()).isNull();
    }
}
