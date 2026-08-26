package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List registered domains.")
final class DomainListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        outputWriter().write(new CliResult.DomainList(domainManager().list()), out());
        return ExitCodes.OK;
    }
}
