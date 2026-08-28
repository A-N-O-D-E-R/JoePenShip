package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.output.CliResult;
import io.virtualization.sdk.iam.ApiCredentialId;
import io.virtualization.sdk.iam.Permissions;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "revoke", description = "Revoke an IAM API credential.")
final class IamCredentialRevokeCommand extends AbstractCliCommand {

    @Parameters(index = "0", paramLabel = "<id>", description = "Credential id.")
    String id;

    @Override
    public Integer call() {
        requirePermission(Permissions.IAM_CREDENTIALS_REVOKE);
        apiCredentialManager().revoke(new ApiCredentialId(id));
        outputWriter().write(new CliResult.Ack(id, "API credential revoked."), out());
        return ExitCodes.OK;
    }
}
