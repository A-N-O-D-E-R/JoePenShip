package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;

@Command(name = "list", description = "List VPSs.")
final class VpsListCommand extends AbstractCliCommand {

    @Override
    public Integer call() {
        outputWriter().write(
                new CliResult.VpsList(vpsManager().list().stream().map(CliResult.VpsView::from).toList()), out());
        return ExitCodes.OK;
    }
}
