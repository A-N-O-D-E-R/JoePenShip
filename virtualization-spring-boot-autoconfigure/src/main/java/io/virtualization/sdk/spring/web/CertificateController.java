package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.certificate.CertificateId;
import io.virtualization.sdk.certificate.CertificateManager;
import io.virtualization.sdk.certificate.CertificateRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * {@code /api/v1/certificates} — over whichever {@link CertificateManager} bean {@code
 * virtualization.certificates.enabled=true} activates (see {@link
 * io.virtualization.sdk.spring.CertificateAutoConfiguration}). Never exposes private key
 * material — {@link CertificateView} only ever carries {@code Certificate} metadata, and this
 * controller never touches a {@code CertificateStore}.
 */
@RestController
@RequestMapping("/api/v1/certificates")
@ConditionalOnBean(CertificateManager.class)
public class CertificateController {

    private final CertificateManager certificateManager;

    public CertificateController(CertificateManager certificateManager) {
        this.certificateManager = certificateManager;
    }

    @GetMapping
    public List<CertificateView> list() {
        return certificateManager.list().stream().map(CertificateView::from).toList();
    }

    @GetMapping("/{id}")
    public CertificateView get(@PathVariable String id) {
        return CertificateView.from(certificateManager.get(new CertificateId(id)));
    }

    @PostMapping
    public CertificateView create(@RequestBody CreateCertificateRequestBody request) {
        Objects.requireNonNull(request.domains(), "'domains' is required");
        Objects.requireNonNull(request.issuer(), "'issuer' is required");

        CertificateRequest.Builder builder = CertificateRequest.builder().domains(request.domains()).issuer(request.issuer());
        if (request.challenge() != null) {
            builder.challenge(request.challenge());
        }
        return CertificateView.from(certificateManager.requestCertificate(builder.build()));
    }

    @PostMapping("/{id}/renew")
    public CertificateView renew(@PathVariable String id) {
        return CertificateView.from(certificateManager.renew(new CertificateId(id)));
    }

    @PostMapping("/{id}/revoke")
    public void revoke(@PathVariable String id) {
        certificateManager.revoke(new CertificateId(id));
    }
}
