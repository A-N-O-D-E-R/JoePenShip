package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualizationProvider;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;

/** Shared plumbing for {@code vm start|stop|reboot|shutdown|destroy}: dispatch, then optionally await completion. */
abstract class AbstractLifecycleCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Virtual machine id.")
    String id;

    @Option(
            names = "--wait",
            negatable = true,
            defaultValue = "true",
            description = "Wait for the operation to complete (default), or return immediately with --no-wait.")
    boolean wait;

    @Option(names = "--timeout", defaultValue = "120", description = "Seconds to wait for completion (default: 120).")
    long timeoutSeconds;

    protected abstract Operation invoke(VirtualizationProvider provider, String id);

    @Override
    public final Integer call() {
        Operation operation = invoke(provider(), id);
        if (wait) {
            operation.await(Duration.ofSeconds(timeoutSeconds));
        }
        outputWriter().write(CliResult.OperationResult.from(operation), out());
        return operation.status() == OperationStatus.FAILED ? ExitCodes.GENERAL_ERROR : ExitCodes.OK;
    }
}
