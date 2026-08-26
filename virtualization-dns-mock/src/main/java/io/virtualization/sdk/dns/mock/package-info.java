/**
 * A real, in-memory {@link io.virtualization.sdk.dns.DnsProvider} implementation — for local
 * development and integration testing without a real DNS backend, not a test double (this module
 * ships in the built artifact; test doubles like {@code FakeDnsProvider} live in {@code src/test}
 * of the modules that need them and never ship). No Cloudflare/Route53/other real-backend
 * dependency here.
 */
package io.virtualization.sdk.dns.mock;
