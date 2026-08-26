package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.domain.Domain;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a registered domain by name.")
final class DomainGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<domain>", description = "Domain name.")
    String domain;

    @Override
    public Integer call() {
        Domain found = domainManager().findByName(domain)
                .orElseThrow(() -> new ResourceNotFoundException("No domain named '" + domain + "'"));
        outputWriter().write(new CliResult.DomainView(found), out());
        return ExitCodes.OK;
    }
}
