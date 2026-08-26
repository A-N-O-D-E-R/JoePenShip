package io.sablier.core;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ReadinessCheckersTest {

    @Test
    void stateOnlyReturnsStateReadinessChecker() {
        assertThat(ReadinessCheckers.stateOnly()).isInstanceOf(StateReadinessChecker.class);
    }

    @Test
    void httpUrlProducesHttpReadinessChecker() {
        ReadinessChecker checker = ReadinessCheckers.fromSpec("http://10.0.0.20:8096/health", Duration.ofSeconds(5));

        assertThat(checker).isInstanceOf(HttpReadinessChecker.class);
        ((HttpReadinessChecker) checker).close();
    }

    @Test
    void httpsUrlProducesHttpReadinessChecker() {
        ReadinessChecker checker = ReadinessCheckers.fromSpec("https://10.0.0.20:8096/health", Duration.ofSeconds(5));

        assertThat(checker).isInstanceOf(HttpReadinessChecker.class);
        ((HttpReadinessChecker) checker).close();
    }

    @Test
    void hostPortProducesTcpReadinessChecker() {
        ReadinessChecker checker = ReadinessCheckers.fromSpec("10.0.0.20:8096", Duration.ofSeconds(5));

        assertThat(checker).isInstanceOf(TcpReadinessChecker.class);
    }

    @Test
    void unrecognizedSpecThrows() {
        assertThatIllegalArgumentException().isThrownBy(() -> ReadinessCheckers.fromSpec("not-a-valid-spec", Duration.ofSeconds(5)));
        assertThatIllegalArgumentException().isThrownBy(() -> ReadinessCheckers.fromSpec("host:notaport", Duration.ofSeconds(5)));
    }
}
