package io.virtualization.sdk.core.image.support;

import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageCapabilities;
import io.virtualization.sdk.core.image.ImageCapability;
import io.virtualization.sdk.core.image.ImageId;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImageQuery;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ImageType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Hand-written test double for {@link ImageProvider}. Serves one canned image
 * ("images:ubuntu/24.04") and enforces the given {@link ImageCapabilities} exactly like a real
 * provider would.
 */
public final class FakeImageProvider implements ImageProvider {

    public static final String NAME = "fake";

    private final ImageCapabilities capabilities;
    private final Image image = new Image(
            new ImageId("fingerprint-1"), "ubuntu/24.04", ImageType.CONTAINER, "x86_64", "ubuntu",
            "ubuntu", "24.04", 512_000L, null, Map.of());
    private final ImageReference reference = new ImageReference(NAME, "images", "ubuntu/24.04");

    public FakeImageProvider(ImageCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ImageCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public List<Image> list() {
        requireCapability(ImageCapability.LIST);
        return List.of(image);
    }

    @Override
    public Optional<Image> get(ImageReference requested) {
        requireCapability(ImageCapability.INSPECT);
        return requested.equals(reference) ? Optional.of(image) : Optional.empty();
    }

    @Override
    public List<Image> search(ImageQuery query) {
        requireCapability(ImageCapability.SEARCH);
        return query.distribution().filter(d -> !d.equals("ubuntu")).isPresent() ? List.of() : List.of(image);
    }

    private void requireCapability(ImageCapability required) {
        if (!capabilities.supports(required)) {
            throw new UnsupportedCapabilityException(
                    "Provider '" + NAME + "' does not support capability " + required);
        }
    }
}
