package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.domain.Domain;
import io.virtualization.sdk.domain.DomainStatus;

import java.time.Instant;

record DomainView(String id, String name, DomainStatus status, String dnsProvider, Instant createdAt) {

    static DomainView from(Domain domain) {
        return new DomainView(domain.id().value(), domain.name(), domain.status(), domain.dnsProvider(), domain.createdAt());
    }
}
