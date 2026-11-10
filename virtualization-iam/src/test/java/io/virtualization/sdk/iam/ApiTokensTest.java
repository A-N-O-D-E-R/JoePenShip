package io.virtualization.sdk.iam;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiTokensTest {

    @Test
    void generateProducesAPrefixedHighEntropyToken() {
        String token = ApiTokens.generate();

        assertThat(token).startsWith("vsdk_");
        assertThat(token.length()).isGreaterThan(40);
    }

    @Test
    void generateNeverRepeats() {
        assertThat(ApiTokens.generate()).isNotEqualTo(ApiTokens.generate());
    }

    @Test
    void hashIsDeterministicAndOneWay() {
        String token = ApiTokens.generate();

        String first = ApiTokens.hash(token);
        String second = ApiTokens.hash(token);

        assertThat(first).isEqualTo(second).doesNotContain(token);
    }

    @Test
    void differentTokensHashDifferently() {
        assertThat(ApiTokens.hash(ApiTokens.generate())).isNotEqualTo(ApiTokens.hash(ApiTokens.generate()));
    }
}
