package io.sablier.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link SessionRepository} backed by an in-memory map. Sessions are lost on application
 * restart — see {@code sablier.startup.recover-sessions} (a later phase's Spring configuration)
 * for the documented restart policy.
 */
public final class InMemorySessionRepository implements SessionRepository {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Override
    public Session save(Session session) {
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public Optional<Session> findById(String id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    public List<Session> findActive() {
        return sessions.values().stream().filter(session -> !session.status().isTerminal()).toList();
    }

    @Override
    public void delete(String id) {
        sessions.remove(id);
    }
}
