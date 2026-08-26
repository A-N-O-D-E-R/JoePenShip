package io.virtualization.sdk.cli;

/** Process exit codes. 0 and 2 match Picocli's own conventions (success / usage error). */
final class ExitCodes {

    static final int OK = 0;
    static final int GENERAL_ERROR = 1;
    static final int USAGE = 2;
    static final int RESOURCE_NOT_FOUND = 3;
    static final int UNSUPPORTED_CAPABILITY = 4;
    static final int AUTH_ERROR = 5;
    static final int CONNECTION_ERROR = 6;
    static final int CONFIGURATION_ERROR = 7;

    private ExitCodes() {}
}
