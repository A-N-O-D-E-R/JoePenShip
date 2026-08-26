package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.spring.VirtualizationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Exposes the {@code /api/v1/images}, {@code /api/v1/workloads}, {@code /api/v1/vps}, {@code
 * /api/v1/domains} and {@code /api/v1/certificates} REST endpoints, when the application is a
 * servlet web app (i.e. depends on {@code spring-boot-starter-web} itself — {@code
 * virtualization-spring-boot-autoconfigure} only depends on it optionally). {@code
 * VpsController}/{@code DomainController}/{@code CertificateController} are each {@code
 * @ConditionalOnBean} their respective manager — they only actually register once the matching
 * {@code virtualization.vps.provider}/{@code virtualization.domains.enabled}/{@code
 * virtualization.certificates.enabled} property is set (see {@link
 * io.virtualization.sdk.spring.VpsAutoConfiguration}/{@link
 * io.virtualization.sdk.spring.DomainAutoConfiguration}/{@link
 * io.virtualization.sdk.spring.CertificateAutoConfiguration}).
 *
 * <p>Registers the controllers by explicit {@link Import} rather than {@code @ComponentScan}:
 * a base-package scan here would also pick up any {@code @Configuration}/{@code @Component}
 * class a consumer happens to put under {@code io.virtualization.sdk.spring.web} (this module's
 * own tests hit exactly that landmine — a test {@code @SpringBootApplication} in a subpackage got
 * scanned and collided with {@link VirtualizationAutoConfiguration}'s beans).
 *
 * <p>Set {@code virtualization.web.enabled=false} to keep the beans out of a web app that doesn't
 * want this API surface exposed.
 */
@AutoConfiguration(after = VirtualizationAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnProperty(prefix = "virtualization.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import({
        ImageController.class, WorkloadController.class, VpsController.class, DomainController.class, CertificateController.class,
        RestExceptionHandler.class
})
public class VirtualizationWebAutoConfiguration {}
