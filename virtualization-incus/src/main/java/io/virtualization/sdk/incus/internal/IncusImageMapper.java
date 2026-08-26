package io.virtualization.sdk.incus.internal;

import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageId;
import io.virtualization.sdk.core.image.ImageType;
import io.virtualization.sdk.incus.client.dto.ImageDto;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Maps Incus image API DTOs onto {@code virtualization-core}'s provider-neutral image model. */
public final class IncusImageMapper {

    private IncusImageMapper() {}

    public static Image toImage(ImageDto dto) {
        Map<String, String> properties = dto.properties() != null ? dto.properties() : Map.of();
        Map<String, String> metadata = new LinkedHashMap<>(properties);
        metadata.put("incus.filename", dto.filename());
        metadata.put("incus.public", String.valueOf(dto.isPublic()));
        metadata.put("incus.cached", String.valueOf(dto.cached()));

        return new Image(
                new ImageId(dto.fingerprint()),
                imageName(dto),
                toType(dto.type()),
                dto.architecture(),
                properties.get("os"),
                properties.get("os"),
                properties.get("release"),
                dto.size(),
                parseInstant(dto.createdAt()),
                metadata);
    }

    private static String imageName(ImageDto dto) {
        if (dto.aliases() != null && !dto.aliases().isEmpty()) {
            return dto.aliases().getFirst().name();
        }
        return dto.filename() != null && !dto.filename().isBlank() ? dto.filename() : dto.fingerprint();
    }

    private static ImageType toType(String type) {
        return switch (type) {
            case "container" -> ImageType.CONTAINER;
            case "virtual-machine" -> ImageType.VIRTUAL_MACHINE;
            case null, default -> ImageType.UNKNOWN;
        };
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
