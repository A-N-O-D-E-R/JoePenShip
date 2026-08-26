package io.virtualization.sdk.vps;

/**
 * How aggressively a VPS provisioning workflow should manage DNS records for {@link
 * VpsSpec#domains()}. Default {@link #NONE} — never modify DNS merely because a VPS exists unless
 * explicitly requested.
 */
public enum DnsProvisioningPolicy {
    /** Don't touch DNS at all. */
    NONE,
    /** Create missing records; never overwrite an existing conflicting one. */
    CREATE,
    /** Create missing records; update a record already owned by this application. */
    CREATE_AND_UPDATE
}
