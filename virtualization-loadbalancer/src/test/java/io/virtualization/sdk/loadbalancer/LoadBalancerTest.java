package io.virtualization.sdk.loadbalancer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class LoadBalancerTest {

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LoadBalancer("", List.of(":443")));
    }

    @Test
    void requiresListenAddresses() {
        assertThatNullPointerException().isThrownBy(() -> new LoadBalancer("web", null));
    }

    @Test
    void rejectsEmptyListenAddresses() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LoadBalancer("web", List.of()));
    }
}
