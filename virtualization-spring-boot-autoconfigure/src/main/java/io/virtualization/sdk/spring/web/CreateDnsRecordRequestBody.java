package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.dns.DnsRecordType;

/** {@code POST /api/v1/domains/{domain}/records} request body. */
record CreateDnsRecordRequestBody(String name, DnsRecordType type, String value, Long ttl, Integer priority) {}
