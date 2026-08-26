package io.virtualization.sdk.cli.certificate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.certificate.CertificateRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link CertificateRepository} backed by a single JSON file — mirrors {@code
 * JsonFileVpsRepository} exactly; {@link Certificate} is already a plain record, so no DTO
 * flattening is needed the way {@code VpsRecord} flattens {@code Vps}.
 */
public final class JsonFileCertificateRepository implements CertificateRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Path file;

    public JsonFileCertificateRepository(Path file) {
        this.file = file;
    }

    @Override
    public synchronized void save(Certificate certificate) {
        Map<CertificateId, Certificate> all = readAll();
        all.put(certificate.id(), certificate);
        writeAll(all);
    }

    @Override
    public synchronized Optional<Certificate> findById(CertificateId id) {
        return Optional.ofNullable(readAll().get(id));
    }

    @Override
    public synchronized List<Certificate> findAll() {
        return List.copyOf(readAll().values());
    }

    @Override
    public synchronized void delete(CertificateId id) {
        Map<CertificateId, Certificate> all = readAll();
        all.remove(id);
        writeAll(all);
    }

    private Map<CertificateId, Certificate> readAll() {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try {
            Certificate[] records = MAPPER.readValue(file.toFile(), Certificate[].class);
            Map<CertificateId, Certificate> map = new LinkedHashMap<>();
            for (Certificate certificate : records) {
                map.put(certificate.id(), certificate);
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read certificate state file '" + file + "'", e);
        }
    }

    private void writeAll(Map<CertificateId, Certificate> all) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writeValue(file.toFile(), all.values());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write certificate state file '" + file + "'", e);
        }
    }
}
