package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;

/**
 * JSON view of an {@link Operation}'s terminal (or in-progress, if the caller didn't wait) state.
 * {@code Operation} itself isn't returned directly — it's a hand-written interface with fluent
 * accessors, not a record, so Jackson has nothing to introspect on it.
 */
record OperationView(String id, OperationStatus status, Double progress, String error) {

    static OperationView from(Operation operation) {
        return new OperationView(
                operation.id(),
                operation.status(),
                operation.progress().isPresent() ? operation.progress().getAsDouble() : null,
                operation.error().map(Throwable::getMessage).orElse(null));
    }
}
