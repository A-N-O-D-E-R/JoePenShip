package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.UserId;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;

@Command(name = "issue", description = "Issue a new API credential for an IAM user.")
final class IamCredentialIssueCommand extends AbstractCliCommand {

    @Option(names = "--owner-id", required = true, description = "Owning user id.")
    String ownerId;

    @Option(names = "--name", required = true, description = "Credential name (a caller-meaningful label, not a secret).")
    String name;

    @Option(names = "--ttl-seconds", description = "Time to live in seconds (default: never expires).")
    Long ttlSeconds;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_CREDENTIALS_ISSUE);
        Duration ttl = ttlSeconds != null ? Duration.ofSeconds(ttlSeconds) : null;
        var issued = apiCredentialManager().issue(new UserId(ownerId), name, ttl);
        outputWriter().write(CliResult.IamIssuedCredentialView.from(issued), out());
        return ExitCodes.OK;
    }
}
