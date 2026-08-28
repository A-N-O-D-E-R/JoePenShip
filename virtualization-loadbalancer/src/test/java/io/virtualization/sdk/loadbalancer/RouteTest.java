package io.virtualization.sdk.loadbalancer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class RouteTest {

    @Test
    void rejectsBlankHost() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Route("web", "", List.of(new Backend("10.0.0.1", 8080)), null));
    }

    @Test
    void rejectsEmptyBackends() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Route("web", "app.example.com", List.of(), null));
    }
}
