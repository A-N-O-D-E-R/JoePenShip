package io.virtualization.sdk.example.spring;

import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.vps.CreateVpsOperation;
import io.virtualization.sdk.vps.Vps;
import io.virtualization.sdk.vps.VpsId;
import io.virtualization.sdk.vps.VpsManager;
import io.virtualization.sdk.vps.VpsSpec;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Demonstrates injecting the auto-configured {@link VpsManager}. Unlike {@link VmService}/{@link
 * ImageService}'s {@link VirtualizationClient} (always created, even with zero providers
 * configured), the {@code VpsManager} bean only exists once {@code virtualization.vps.provider}
 * names a configured provider — so it's wired as {@link Optional} rather than a hard constructor
 * dependency, and an app that hasn't opted into VPS management still starts cleanly.
 */
@Service
public class VpsService {

    private final Optional<VpsManager> manager;

    public VpsService(Optional<VpsManager> manager) {
        this.manager = manager;
    }

    public List<Vps> list() {
        return manager.map(VpsManager::list).orElseGet(List::of);
    }

    public CreateVpsOperation create(VpsSpec spec) {
        return manager.orElseThrow(VpsService::notConfigured).create(spec);
    }

    public Vps get(VpsId id) {
        return manager.orElseThrow(VpsService::notConfigured).get(id);
    }

    private static IllegalStateException notConfigured() {
        return new IllegalStateException("VPS management isn't configured — set virtualization.vps.provider");
    }
}
