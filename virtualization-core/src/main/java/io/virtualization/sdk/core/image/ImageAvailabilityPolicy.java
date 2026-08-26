package io.virtualization.sdk.core.image;

/**
 * Governs whether creating a workload from an {@link ImageReference} may pull the image first.
 * Consumed by the create-from-image flow added in a later phase; kept here now so provider
 * capability checks can already be written against it.
 */
public enum ImageAvailabilityPolicy {

    /** Fail if the image is not already present locally. */
    REQUIRE_LOCAL,

    /** Pull the image first if it is not already present locally. This is the default. */
    PULL_IF_MISSING,

    /** Always pull the image, even if a local copy already exists. */
    ALWAYS_REFRESH
}
