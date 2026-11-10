package io.virtualization.sdk.backup;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

/**
 * A backup backend (Incus, ...). Implementations live in their own provider modules — this
 * module never depends on one.
 */
public interface BackupProvider {

    /** @throws ResourceNotFoundException if {@code instanceId} is unknown */
    List<Snapshot> listSnapshots(String instanceId);

    /** @throws ResourceNotFoundException if {@code instanceId} is unknown */
    Snapshot createSnapshot(String instanceId, String name, boolean stateful);

    /** @throws ResourceNotFoundException if {@code instanceId} or the snapshot is unknown */
    void restoreSnapshot(String instanceId, String name);

    /** @throws ResourceNotFoundException if {@code instanceId} or the snapshot is unknown */
    void deleteSnapshot(String instanceId, String name);

    /** @throws ResourceNotFoundException if {@code instanceId} is unknown */
    List<Backup> listBackups(String instanceId);

    /** @throws ResourceNotFoundException if {@code instanceId} is unknown */
    Backup createBackup(String instanceId, String name, Instant expiresAt);

    /** Caller closes the returned stream. @throws ResourceNotFoundException if {@code instanceId} or the backup is unknown */
    InputStream downloadBackup(String instanceId, String name);

    /** @throws ResourceNotFoundException if {@code instanceId} or the backup is unknown */
    void deleteBackup(String instanceId, String name);
}
