package io.virtualization.sdk.vps.support;

import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageCapabilities;
import io.virtualization.sdk.core.image.ImageId;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImageQuery;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ImageType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Hand-written test double for {@link ImageProvider}. Only identifiers registered via {@link #knows} resolve. */
public final class FakeImageProvider implements ImageProvider {

    public static final String NAME = "fake";

    private final Set<String> knownIdentifiers = ConcurrentHashMap.newKeySet();

    public FakeImageProvider knows(String identifier) {
        knownIdentifiers.add(identifier);
        return this;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ImageCapabilities capabilities() {
        return ImageCapabilities.of();
    }

    @Override
    public List<Image> list() {
        return List.of();
    }

    @Override
    public Optional<Image> get(ImageReference reference) {
        if (!knownIdentifiers.contains(reference.identifier())) {
            return Optional.empty();
        }
        return Optional.of(new Image(
                new ImageId("fp-" + reference.identifier()), reference.identifier(), ImageType.CONTAINER, "x86_64",
                "ubuntu", "ubuntu", "24.04", 1_024, Instant.now(), Map.of()));
    }

    @Override
    public List<Image> search(ImageQuery query) {
        return List.of();
    }
}
