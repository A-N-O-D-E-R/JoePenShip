package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordType;

record DnsRecordView(String id, String zone, String name, DnsRecordType type, String value, Long ttl, Integer priority) {

    static DnsRecordView from(DnsRecord record) {
        return new DnsRecordView(record.id(), record.zone(), record.name(), record.type(), record.value(), record.ttl(), record.priority());
    }
}
