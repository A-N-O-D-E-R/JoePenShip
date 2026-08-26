package io.virtualization.sdk.core.image;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageAvailabilityPolicyTest {

    @Test
    void declaresTheThreeSupportedPolicies() {
        assertThat(ImageAvailabilityPolicy.values()).containsExactly(
                ImageAvailabilityPolicy.REQUIRE_LOCAL,
                ImageAvailabilityPolicy.PULL_IF_MISSING,
                ImageAvailabilityPolicy.ALWAYS_REFRESH);
    }
}
