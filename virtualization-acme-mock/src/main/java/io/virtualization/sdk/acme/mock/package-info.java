/**
 * A real {@link io.virtualization.sdk.certificate.AcmeProvider} implementation —
 * {@link io.virtualization.sdk.acme.mock.Dns01AcmeProvider} performs the full DNS-01 challenge
 * dance (create TXT record, "validate", issue, clean up — even on failure) over a composed {@link
 * io.virtualization.sdk.dns.DnsProvider}. For local development and integration testing without a
 * real certificate authority, not a test double (this module ships in the built artifact). No
 * Let's Encrypt or real ACME wire protocol here.
 */
package io.virtualization.sdk.acme.mock;
