package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.domain.Domain;
import io.virtualization.sdk.domain.DomainManager;
import io.virtualization.sdk.dns.DnsRecordSpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * {@code /api/v1/domains} — domain registration and DNS record management, over whichever {@link
 * DomainManager} bean {@code virtualization.domains.enabled=true} activates (see {@link
 * io.virtualization.sdk.spring.DomainAutoConfiguration}). Only registers when a {@code
 * DomainManager} bean exists.
 */
@RestController
@RequestMapping("/api/v1/domains")
@ConditionalOnBean(DomainManager.class)
public class DomainController {

    private final DomainManager domainManager;

    public DomainController(DomainManager domainManager) {
        this.domainManager = domainManager;
    }

    @GetMapping
    public List<DomainView> list() {
        return domainManager.list().stream().map(DomainView::from).toList();
    }

    @GetMapping("/{domain}")
    public DomainView get(@PathVariable String domain) {
        return DomainView.from(requireDomain(domain));
    }

    @GetMapping("/{domain}/records")
    public List<DnsRecordView> listRecords(@PathVariable String domain) {
        Domain resolved = requireDomain(domain);
        return domainManager.listRecords(resolved.id()).stream().map(DnsRecordView::from).toList();
    }

    @PostMapping("/{domain}/records")
    public DnsRecordView createRecord(@PathVariable String domain, @RequestBody CreateDnsRecordRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        Objects.requireNonNull(request.type(), "'type' is required");
        Objects.requireNonNull(request.value(), "'value' is required");

        Domain resolved = requireDomain(domain);
        DnsRecordSpec spec = new DnsRecordSpec(request.name(), request.type(), request.value(), request.ttl(), request.priority());
        return DnsRecordView.from(domainManager.createRecord(resolved.id(), spec));
    }

    @DeleteMapping("/{domain}/records/{id}")
    public void deleteRecord(@PathVariable String domain, @PathVariable String id) {
        Domain resolved = requireDomain(domain);
        domainManager.deleteRecord(resolved.id(), id);
    }

    private Domain requireDomain(String name) {
        return domainManager.findByName(name).orElseThrow(() -> new ResourceNotFoundException("No domain '" + name + "'"));
    }
}
