package io.virtualization.sdk.spring.web;

import java.time.Duration;

/**
 * Pull/import/create-workload are async {@code Operation}s at the SDK level, but this v1 REST API
 * exposes them synchronously: the controller awaits completion (bounded by this timeout) and
 * returns the terminal state in the response body. A future phase can add a polling endpoint for
 * true fire-and-forget semantics with live progress; nothing here needs that yet.
 */
final class WebDefaults {

    static final Duration OPERATION_TIMEOUT = Duration.ofMinutes(5);

    private WebDefaults() {}
}
