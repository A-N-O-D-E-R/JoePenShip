package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.vps.VpsId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "get", description = "Get a VPS by id.")
final class VpsGetCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "VPS id.")
    String id;

    @Override
    public Integer call() {
        outputWriter().write(CliResult.VpsView.from(vpsManager().get(new VpsId(id))), out());
        return ExitCodes.OK;
    }
}
