package io.virtualization.sdk.vps;

/**
 * A VPS's network attachment. All fields nullable — a null {@code ipv4}/{@code ipv6} means DHCP;
 * static addressing is only used where the underlying provider supports it.
 */
public record NetworkConfiguration(String network, String ipv4, String ipv6, String hostname) {

    /** Pure DHCP: attach to the provider's default network, no static addressing, no hostname override. */
    public static final NetworkConfiguration UNSPECIFIED = new NetworkConfiguration(null, null, null, null);
}
