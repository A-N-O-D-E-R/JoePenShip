package io.sablier.core;

import java.util.List;
import java.util.Optional;

/** Persists {@link Session}s. Designed so an SQL-backed implementation can replace {@link InMemorySessionRepository} later. */
public interface SessionRepository {

    Session save(Session session);

    Optional<Session> findById(String id);

    /** @return every session whose {@link SessionStatus#isTerminal()} is {@code false} */
    List<Session> findActive();

    void delete(String id);
}
