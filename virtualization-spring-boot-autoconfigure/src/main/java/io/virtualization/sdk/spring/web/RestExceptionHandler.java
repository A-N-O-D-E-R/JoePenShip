package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.AuthorizationException;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import io.virtualization.sdk.vps.InvalidVpsStateException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps SDK exceptions to HTTP status codes for the {@code .web} controllers — mirrors {@code
 * virtualization-cli}'s {@code CliExceptionHandler}, HTTP status in place of process exit code.
 *
 * <p>{@link ConfigurationException} maps to 400, not 500: in this REST API it's almost always
 * triggered by an unknown {@code provider} name from the caller, a client input error, not a
 * server misconfiguration.
 */
@RestControllerAdvice
class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorBody> notFound(ResourceNotFoundException e) {
        return status(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(InvalidVpsStateException.class)
    ResponseEntity<ErrorBody> invalidVpsState(InvalidVpsStateException e) {
        return status(HttpStatus.CONFLICT, e);
    }

    /** {@code DomainManager}/{@code CertificateManager} both use this for state-conflict cases (no DNS provider associated yet, renewing a REVOKED/FAILED certificate). */
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ErrorBody> illegalState(IllegalStateException e) {
        return status(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(UnsupportedCapabilityException.class)
    ResponseEntity<ErrorBody> unsupported(UnsupportedCapabilityException e) {
        return status(HttpStatus.NOT_IMPLEMENTED, e);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorBody> authentication(AuthenticationException e) {
        return status(HttpStatus.UNAUTHORIZED, e);
    }

    @ExceptionHandler(AuthorizationException.class)
    ResponseEntity<ErrorBody> authorization(AuthorizationException e) {
        return status(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler(ConnectionException.class)
    ResponseEntity<ErrorBody> connection(ConnectionException e) {
        return status(HttpStatus.BAD_GATEWAY, e);
    }

    @ExceptionHandler(ConfigurationException.class)
    ResponseEntity<ErrorBody> configuration(ConfigurationException e) {
        return status(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    ResponseEntity<ErrorBody> badRequest(RuntimeException e) {
        return status(HttpStatus.BAD_REQUEST, e);
    }

    @ExceptionHandler(VirtualizationException.class)
    ResponseEntity<ErrorBody> virtualization(VirtualizationException e) {
        return status(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    private static ResponseEntity<ErrorBody> status(HttpStatus status, Exception e) {
        return ResponseEntity.status(status).body(new ErrorBody(e.getMessage()));
    }
}
