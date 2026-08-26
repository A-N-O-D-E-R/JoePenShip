package io.virtualization.sdk.cli;

import picocli.CommandLine;

/** Entry point for the {@code virtualization} executable. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        int exitCode = new CommandLine(new VirtualizationCli())
                .setExecutionExceptionHandler(new CliExceptionHandler())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }
}
