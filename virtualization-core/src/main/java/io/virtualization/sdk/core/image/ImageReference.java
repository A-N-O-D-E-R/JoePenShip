package io.virtualization.sdk.core.image;

import java.util.Objects;

/**
 * A provider-scoped locator for an image, as supplied by a caller — as opposed to {@link ImageId},
 * which identifies an already-resolved {@link Image}.
 *
 * <p>Examples: {@code ImageReference("incus", "images", "ubuntu/24.04")} (remote + alias/fingerprint),
 * {@code ImageReference("docker", null, "library/ubuntu:24.04")} (repository:tag, no separate remote).
 *
 * @param provider   the {@link ImageProvider#name()} this reference is scoped to
 * @param remote     the remote/registry/storage the image lives in, or {@code null} if the
 *                    provider has no such concept
 * @param identifier the provider-specific identifier within that remote (alias, fingerprint,
 *                    repository:tag, digest, volume id, ...)
 */
public record ImageReference(String provider, String remote, String identifier) {

    public ImageReference {
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(identifier, "identifier must not be null");
        if (provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (identifier.isBlank()) {
            throw new IllegalArgumentException("identifier must not be blank");
        }
    }

    public ImageReference(String provider, String identifier) {
        this(provider, null, identifier);
    }
}
