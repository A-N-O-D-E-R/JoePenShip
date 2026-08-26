package io.virtualization.sdk.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(
        name = "certificate",
        description = "Manage provider-neutral TLS certificates via ACME.",
        subcommands = {
            CertificateListCommand.class, CertificateGetCommand.class, CertificateRequestCommand.class,
            CertificateRenewCommand.class, CertificateRevokeCommand.class, CertificateExportCommand.class
        })
final class CertificateCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        spec.commandLine().usage(spec.commandLine().getOut());
        return ExitCodes.OK;
    }
}
