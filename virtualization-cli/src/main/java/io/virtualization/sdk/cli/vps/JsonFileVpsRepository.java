package io.virtualization.sdk.cli.vps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link VpsRepository} backed by a single JSON file — the CLI has no long-lived process to keep
 * an {@code InMemoryVpsRepository} alive between invocations, so {@code vps create} in one process
 * and {@code vps list}/{@code get}/lifecycle commands in the next need somewhere durable to read
 * from.
 *
 * <p>ponytail: whole-file read-modify-write under an in-process lock only, no OS-level file
 * locking — fine for a CLI tool driven sequentially by one user; two CLI processes racing on the
 * same file can lose an update. Upgrade to {@code FileChannel#lock} if that ever matters.
 */
public final class JsonFileVpsRepository implements VpsRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Path file;

    public JsonFileVpsRepository(Path file) {
        this.file = file;
    }

    @Override
    public synchronized void save(Vps vps) {
        Map<VpsId, VpsRecord> all = readAll();
        all.put(vps.id(), VpsRecord.from(vps));
        writeAll(all);
    }

    @Override
    public synchronized Optional<Vps> findById(VpsId id) {
        return Optional.ofNullable(readAll().get(id)).map(VpsRecord::toVps);
    }

    @Override
    public synchronized List<Vps> findAll() {
        return readAll().values().stream().map(VpsRecord::toVps).toList();
    }

    @Override
    public synchronized void delete(VpsId id) {
        Map<VpsId, VpsRecord> all = readAll();
        all.remove(id);
        writeAll(all);
    }

    private Map<VpsId, VpsRecord> readAll() {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try {
            VpsRecord[] records = MAPPER.readValue(file.toFile(), VpsRecord[].class);
            Map<VpsId, VpsRecord> map = new LinkedHashMap<>();
            for (VpsRecord record : records) {
                map.put(record.id(), record);
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read VPS state file '" + file + "'", e);
        }
    }

    private void writeAll(Map<VpsId, VpsRecord> all) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writeValue(file.toFile(), all.values());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write VPS state file '" + file + "'", e);
        }
    }
}
