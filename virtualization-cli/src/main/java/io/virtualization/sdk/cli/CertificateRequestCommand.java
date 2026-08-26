package io.virtualization.sdk.cli;

import io.virtualization.sdk.certificate.Certificate;
import io.virtualization.sdk.certificate.CertificateRequest;
import io.virtualization.sdk.certificate.ChallengeType;
import io.virtualization.sdk.cli.certificate.Passphrases;
import io.virtualization.sdk.cli.output.CliResult;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "request", description = "Request a new certificate via ACME DNS-01.")
final class CertificateRequestCommand extends AbstractCliCommand {

    @Option(names = "--domain", required = true, description = "Domain to cover, repeatable.")
    List<String> domains;

    @Option(names = "--dns-provider", description = "Configured DNS provider name (existence-checked only).")
    String dnsProvider;

    @Option(names = "--challenge", defaultValue = "DNS_01", description = "ACME challenge type: ${COMPLETION-CANDIDATES} (default: DNS_01).")
    ChallengeType challenge;

    @Option(
            names = "--passphrase",
            description = "Passphrase to encrypt certificate material with (prefer the VIRTUALIZATION_CERTIFICATE_PASSPHRASE "
                    + "environment variable — a flag value may land in shell history).")
    String passphrase;

    @Override
    public Integer call() {
        String issuer = requireProviderName();
        if (dnsProvider != null) {
            dnsProviderRegistry().get(dnsProvider); // existence-check only; ConfigurationException propagates unrewrapped
        }
        CertificateRequest request = CertificateRequest.builder().domains(domains).issuer(issuer).challenge(challenge).build();
        Certificate certificate = certificateManager(Passphrases.resolve(passphrase)).requestCertificate(request);
        outputWriter().write(new CliResult.CertificateView(certificate), out());
        return ExitCodes.OK;
    }
}
