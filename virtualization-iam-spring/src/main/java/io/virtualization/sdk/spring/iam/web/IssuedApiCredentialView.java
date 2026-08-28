package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.ApiCredentialManager;

/** The raw token, shown exactly once — {@link ApiCredentialManager#issue}'s full result. */
public record IssuedApiCredentialView(String token, ApiCredentialView credential) {

    public static IssuedApiCredentialView from(ApiCredentialManager.IssuedApiCredential issued) {
        return new IssuedApiCredentialView(issued.rawToken(), ApiCredentialView.from(issued.credential()));
    }
}
