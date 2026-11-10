/**
 * Provider-neutral network/IPAM/firewall model — {@link io.virtualization.sdk.network.Network},
 * {@link io.virtualization.sdk.network.Subnet} (real IPv4 CIDR address math via a package-private
 * {@code CidrBlock}), {@link io.virtualization.sdk.network.IpamService} (allocate/release),
 * {@link io.virtualization.sdk.network.FirewallRule} (a rule catalog — no provider enforces it
 * yet). Pure Java, no framework dependency, same core tier as {@code virtualization-project} and
 * {@code virtualization-iam}.
 */
package io.virtualization.sdk.network;
