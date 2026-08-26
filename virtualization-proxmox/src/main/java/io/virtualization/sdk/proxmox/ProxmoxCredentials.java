package io.virtualization.sdk.proxmox;

import java.util.Objects;

/**
 * Proxmox API token credentials, in the form Proxmox expects: {@code tokenId} is
 * {@code user@realm!tokenName}, {@code tokenSecret} is the token's UUID secret.
 *
 * <p>{@link #toString()} is overridden to redact the secret — the default record
 * {@code toString()} would otherwise print it, which section 9 of the SDK spec forbids
 * ("Never log the token secret").
 */
public record ProxmoxCredentials(String tokenId, String tokenSecret) {

    public ProxmoxCredentials {
        Objects.requireNonNull(tokenId, "tokenId must not be null");
        Objects.requireNonNull(tokenSecret, "tokenSecret must not be null");
        if (tokenId.isBlank()) {
            throw new IllegalArgumentException("tokenId must not be blank");
        }
        if (tokenSecret.isBlank()) {
            throw new IllegalArgumentException("tokenSecret must not be blank");
        }
    }

    /** The value of the HTTP {@code Authorization} header Proxmox expects for this token. */
    public String toAuthorizationHeaderValue() {
        return "PVEAPIToken=" + tokenId + "=" + tokenSecret;
    }

    @Override
    public String toString() {
        return "ProxmoxCredentials[tokenId=" + tokenId + ", tokenSecret=****]";
    }
}
