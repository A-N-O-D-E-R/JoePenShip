/**
 * Provider-neutral identity and access management: {@link io.virtualization.sdk.iam.User}s,
 * {@link io.virtualization.sdk.iam.Role}s, {@link io.virtualization.sdk.iam.Group}s and
 * {@link io.virtualization.sdk.iam.ApiCredential}s, plus an {@link
 * io.virtualization.sdk.iam.AuthorizationService} resolving effective permissions. Pure Java, no
 * framework dependency — durable storage lives in {@code virtualization-iam-store}, Spring/CLI
 * wiring in {@code virtualization-iam-spring}/{@code virtualization-cli}.
 */
package io.virtualization.sdk.iam;
