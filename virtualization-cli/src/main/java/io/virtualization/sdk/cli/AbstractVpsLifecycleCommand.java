package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsManager;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.Duration;

/** Shared plumbing for {@code vps start|stop|restart|shutdown|destroy}: dispatch, then optionally await completion. */
abstract class AbstractVpsLifecycleCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "VPS id.")
    String id;

    @Option(
            names = "--wait",
            negatable = true,
            defaultValue = "true",
            description = "Wait for the operation to complete (default), or return immediately with --no-wait.")
    boolean wait;

    @Option(names = "--timeout", defaultValue = "120", description = "Seconds to wait for completion (default: 120).")
    long timeoutSeconds;

    protected abstract Operation invoke(VpsManager manager, VpsId id);

    @Override
    public final Integer call() {
        VpsManager manager = vpsManager();
        VpsId vpsId = new VpsId(id);
        Operation operation = invoke(manager, vpsId);
        if (wait) {
            operation.await(Duration.ofSeconds(timeoutSeconds));
            // the CLI process exits right after this — force the reconciled terminal state into
            // the JSON file now, since there's no later invocation of *this* VpsManager to do it.
            manager.get(vpsId);
        }
        outputWriter().write(CliResult.OperationResult.from(operation), out());
        return operation.status() == OperationStatus.FAILED ? ExitCodes.GENERAL_ERROR : ExitCodes.OK;
    }
}
