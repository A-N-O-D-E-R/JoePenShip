package io.virtualization.sdk.example.spring;

import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualizationClient;
import org.springframework.stereotype.Service;

import java.util.List;

/** Demonstrates injecting the auto-configured {@link VirtualizationClient} into an application bean. */
@Service
public class VmService {

    private final VirtualizationClient client;

    public VmService(VirtualizationClient client) {
        this.client = client;
    }

    public List<VirtualMachine> list(String providerName) {
        return client.provider(providerName).listVirtualMachines();
    }
}
