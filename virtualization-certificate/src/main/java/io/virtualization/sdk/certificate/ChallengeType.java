package io.virtualization.sdk.certificate;

/** {@code HTTP_01} is named now but not yet supported by any {@link AcmeProvider} — kept as a future option. */
public enum ChallengeType {
    DNS_01,
    HTTP_01
}
