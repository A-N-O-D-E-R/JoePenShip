package io.virtualization.sdk.dns;

/** {@code NS}/{@code SRV} deferred until a caller actually needs them. */
public enum DnsRecordType {
    A,
    AAAA,
    CNAME,
    TXT,
    MX
}
