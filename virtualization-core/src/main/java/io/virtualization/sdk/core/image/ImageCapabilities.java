package io.virtualization.sdk.core.image;

import java.util.EnumSet;
import java.util.Set;

/** The set of {@link ImageCapability} values an {@link ImageProvider} supports. */
public final class ImageCapabilities {

    private final Set<ImageCapability> capabilities;

    private ImageCapabilities(Set<ImageCapability> capabilities) {
        this.capabilities = capabilities;
    }

    public static ImageCapabilities of(ImageCapability... capabilities) {
        EnumSet<ImageCapability> set = EnumSet.noneOf(ImageCapability.class);
        for (ImageCapability capability : capabilities) {
            set.add(capability);
        }
        return new ImageCapabilities(Set.copyOf(set));
    }

    public boolean supports(ImageCapability capability) {
        return capabilities.contains(capability);
    }

    public Set<ImageCapability> all() {
        return capabilities;
    }
}
