package io.virtualization.sdk.vps;

/**
 * Well-known {@code WorkloadSpec.providerOptions()} keys {@link DefaultVpsProvisioner} packs
 * {@link VpsSpec} fields that {@code WorkloadSpec} has no native field for into. A provider's
 * {@code VirtualizationProvider.createFromImage} implementation reads these to actually configure
 * SSH/cloud-init/hostname/addressing/location — this module never talks to a specific backend, it
 * just documents the contract. {@code "project"} deliberately matches the plain (unprefixed) key
 * the CLI's {@code WorkloadCreateCommand --project} flag already uses, for one consistent
 * provider-options namespace across the SDK rather than a competing one.
 */
public final class VpsProviderOptionKeys {

    public static final String SSH_PUBLIC_KEYS = "sshPublicKeys";
    public static final String CLOUD_INIT = "cloudInit";
    public static final String HOSTNAME = "hostname";
    public static final String IPV4 = "ipv4";
    public static final String IPV6 = "ipv6";
    public static final String LOCATION = "location";
    public static final String STORAGE_POOL = "storagePool";
    public static final String VOLUME_TYPE = "volumeType";
    public static final String PROJECT = "project";

    private VpsProviderOptionKeys() {}
}
