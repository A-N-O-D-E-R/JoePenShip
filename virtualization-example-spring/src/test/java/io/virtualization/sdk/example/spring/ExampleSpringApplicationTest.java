package io.virtualization.sdk.example.spring;

import io.virtualization.sdk.core.VirtualizationClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ExampleSpringApplicationTest {

    @Autowired
    private VirtualizationClient client;

    @Autowired
    private VmService vmService;

    @Autowired
    private VpsService vpsService;

    @Test
    void contextLoadsWithNoProvidersConfigured() {
        assertThat(client.providers()).isEmpty();
        assertThat(vmService).isNotNull();
    }

    @Test
    void vpsServiceStartsWithoutAVpsManagerBeanWhenNoneIsConfigured() {
        assertThat(vpsService.list()).isEmpty();
    }
}
