package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.AuthorizationException;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.exception.VirtualizationException;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

import java.io.PrintWriter;

/**
 * Maps SDK exceptions to process exit codes and prints a one-line message to stderr — never a raw
 * stack trace for a known SDK failure, and nothing on stdout (stdout is reserved for {@code
 * --output json}/{@code yaml} results).
 */
final class CliExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
        PrintWriter err = cmd.getErr();
        int exitCode = switch (ex) {
            case ResourceNotFoundException e -> report(err, e, ExitCodes.RESOURCE_NOT_FOUND);
            case UnsupportedCapabilityException e -> report(err, e, ExitCodes.UNSUPPORTED_CAPABILITY);
            case AuthenticationException e -> report(err, e, ExitCodes.AUTH_ERROR);
            case AuthorizationException e -> report(err, e, ExitCodes.AUTH_ERROR);
            case ConnectionException e -> report(err, e, ExitCodes.CONNECTION_ERROR);
            case ConfigurationException e -> report(err, e, ExitCodes.CONFIGURATION_ERROR);
            case IllegalStateException e -> report(err, e, ExitCodes.GENERAL_ERROR);
            case VirtualizationException e -> report(err, e, ExitCodes.GENERAL_ERROR);
            default -> {
                err.println("Unexpected error: " + ex.getMessage());
                yield ExitCodes.GENERAL_ERROR;
            }
        };
        err.flush();
        return exitCode;
    }

    private static int report(PrintWriter err, Exception e, int exitCode) {
        err.println("Error: " + e.getMessage());
        return exitCode;
    }
}
