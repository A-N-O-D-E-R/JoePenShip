package io.virtualization.sdk.example.spring;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Minimal runnable example of {@code virtualization-spring-boot-starter}. With no {@code
 * virtualization.providers.*} configured (the default {@code application.yml} ships empty — see
 * that file for a copy-pasteable example), the demo runner below reports that gracefully rather
 * than failing; this is what keeps {@code ./mvnw clean verify} free of any dependency on a real
 * Proxmox, Incus or QEMU instance.
 */
@SpringBootApplication
public class ExampleSpringApplication {

    private static final Logger log = LoggerFactory.getLogger(ExampleSpringApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ExampleSpringApplication.class, args);
    }

    @Bean
    CommandLineRunner listVirtualMachinesOnStartup(VmService vmService) {
        return args -> {
            try {
                var vms = vmService.list("production");
                log.info("production has {} virtual machine(s): {}", vms.size(), vms);
            } catch (ConfigurationException e) {
                log.info("No 'production' provider configured yet — add one under virtualization.providers "
                        + "in application.yml to see this list a real VM inventory. ({})", e.getMessage());
            }
        };
    }

    /** Only the {@code incus} provider type implements image management so far — see {@code containers} in application.yml. */
    @Bean
    CommandLineRunner listImagesOnStartup(ImageService imageService) {
        return args -> {
            try {
                var images = imageService.list("containers");
                log.info("containers has {} image(s): {}", images.size(), images);
            } catch (ConfigurationException e) {
                log.info("No 'containers' (incus) provider configured yet — add one under virtualization.providers "
                        + "in application.yml to see this list real images. ({})", e.getMessage());
            }
        };
    }

    /** {@link VpsService} degrades gracefully (empty list) rather than throwing when no {@code virtualization.vps.provider} is set. */
    @Bean
    CommandLineRunner listVpsOnStartup(VpsService vpsService) {
        return args -> {
            var vpsList = vpsService.list();
            if (vpsList.isEmpty()) {
                log.info("No VPS management configured yet — set virtualization.vps.provider in application.yml to enable it.");
            } else {
                log.info("{} VPS(s): {}", vpsList.size(), vpsList);
            }
        };
    }
}
