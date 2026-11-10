package io.virtualization.sdk.cli.iam;

import io.virtualization.sdk.core.exception.AuthenticationException;

/**
 * Resolves the API token a CLI invocation authenticates with — a {@code --api-token} flag takes
 * precedence but {@link #ENV_VAR} is preferred, since a flag value can land in shell history.
 * Mirrors {@code io.virtualization.sdk.cli.certificate.Passphrases} exactly.
 */
public final class ApiCredentials {

    public static final String ENV_VAR = "VIRTUALIZATION_API_TOKEN";

    private ApiCredentials() {}

    /** @throws AuthenticationException if neither {@code cliValue} nor {@link #ENV_VAR} is set — no credentials at all is an auth failure, not a config error */
    public static String resolve(String cliValue) {
        if (cliValue != null && !cliValue.isBlank()) {
            return cliValue;
        }
        String envValue = System.getenv(ENV_VAR);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        throw new AuthenticationException("No API token available — pass --api-token or set " + ENV_VAR + ".");
    }
}
