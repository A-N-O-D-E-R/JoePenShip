package io.sablier.core;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class InMemorySessionRepositoryTest {

    private final InMemorySessionRepository repository = new InMemorySessionRepository();

    private static Session session(String id, SessionStatus status) {
        Instant now = Instant.now();
        return new Session(id, "media", Optional.of("w-1"), now, now.plusSeconds(60), status);
    }

    @Test
    void saveAndFindById() {
        Session session = session("s-1", SessionStatus.ACTIVE);

        repository.save(session);

        assertThat(repository.findById("s-1")).contains(session);
    }

    @Test
    void findByIdReturnsEmptyForUnknownId() {
        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void deleteRemovesSession() {
        repository.save(session("s-1", SessionStatus.ACTIVE));

        repository.delete("s-1");

        assertThat(repository.findById("s-1")).isEmpty();
    }

    @Test
    void findActiveExcludesTerminalStatuses() {
        repository.save(session("active", SessionStatus.ACTIVE));
        repository.save(session("starting", SessionStatus.STARTING));
        repository.save(session("expired", SessionStatus.EXPIRED));
        repository.save(session("terminated", SessionStatus.TERMINATED));
        repository.save(session("failed", SessionStatus.FAILED));

        assertThat(repository.findActive()).extracting(Session::id).containsExactlyInAnyOrder("active", "starting");
    }
}
