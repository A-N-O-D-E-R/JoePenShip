package io.virtualization.sdk.proxmox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualMachineState;
import io.virtualization.sdk.core.firewall.WorkloadFirewall;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.ProviderOptions;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;
import io.virtualization.sdk.proxmox.client.ProxmoxApiClient;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Against a real Proxmox VE node. Opt-in:
 * {@code PROXMOX_LAB_TOKEN='user@realm!id=secret' mvn test -Dtest=ProxmoxLabIT
 * -Dproxmox.lab=https://host:8006 -Dproxmox.template=9000 [-Dproxmox.node=pve]}.
 * Creates, starts, firewalls, stops and destroys one VM.
 */
class ProxmoxLabIT {

    @Test
    void createFirewallStartStopDestroy() {
        String endpoint = System.getProperty("proxmox.lab");
        String token = System.getenv("PROXMOX_LAB_TOKEN");
        assumeTrue(endpoint != null && token != null, "set -Dproxmox.lab and PROXMOX_LAB_TOKEN to run");
        String template = System.getProperty("proxmox.template", "9000");
        int eq = token.indexOf('=');
        var config = new ProxmoxClientConfig(URI.create(endpoint), new ProxmoxCredentials(token.substring(0, eq), token.substring(eq + 1)),
                false, Duration.ofSeconds(10), Duration.ofSeconds(60), Duration.ofMillis(500));
        var client = new ProxmoxApiClient(config);
        var provider = new ProxmoxProvider(client, config.taskPollInterval());
        String name = "sdk-lab-" + Long.toHexString(System.nanoTime());
        String key = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIBzYuLnU2GRsxo7AeWkb1gWuZqMI3gdEfJmd2BV4KVPv lab@sdk";

        Map<String, Object> options = new java.util.HashMap<>(Map.of("diskGb", 5));
        if (System.getProperty("proxmox.node") != null) {
            options.put("node", System.getProperty("proxmox.node"));
        }
        var spec = WorkloadSpec.builder(name, WorkloadType.VIRTUAL_MACHINE)
                .resources(new ComputeResources(1, 768))
                .configuration(Map.of("sshAuthorizedKeys", key))
                .labels(Map.of("sdk-lab", name))
                .providerOptions(ProviderOptions.of(options))
                .build();
        var created = provider.createFromImage(new ImageReference("proxmox", template), spec);
        String vmid = null;
        try {
            assertThat(created.await(Duration.ofMinutes(5))).as(created.error().map(Throwable::getMessage).orElse(""))
                    .isEqualTo(OperationStatus.SUCCEEDED);
            vmid = created.workloadId().orElseThrow();
            String base = "/nodes/" + nodeOf(client, vmid) + "/qemu/" + vmid;

            JsonNode cfg = client.getData(base + "/config");
            assertThat(cfg.path("cores").asInt()).isEqualTo(1);
            assertThat(cfg.path("memory").asText()).isEqualTo("768");
            assertThat(cfg.path("scsi0").asText()).contains("size=5G");
            assertThat(URLDecoder.decode(cfg.path("sshkeys").asText(), StandardCharsets.UTF_8).strip()).isEqualTo(key);
            assertThat(cfg.path("description").asText()).contains("sdk-lab=" + name);

            var in = WorkloadFirewall.Direction.INBOUND;
            var fw = new WorkloadFirewall(List.of(
                    new WorkloadFirewall.Rule(in, "tcp", 22, 22, List.of("10.0.0.0/8", "2001:db8::/32"), "ssh"),
                    new WorkloadFirewall.Rule(in, "icmp", 8, null, List.of("0.0.0.0/0"), null),
                    new WorkloadFirewall.Rule(WorkloadFirewall.Direction.OUTBOUND, null, null, null, List.of("0.0.0.0/0"), null)));
            var applied = provider.applyFirewall(vmid, fw);
            assertThat(applied.await(Duration.ofMinutes(1))).as(applied.error().map(Throwable::getMessage).orElse(""))
                    .isEqualTo(OperationStatus.SUCCEEDED);
            assertThat(client.getData(base + "/config").path("net0").asText()).contains("firewall=1");
            JsonNode opts = client.getData(base + "/firewall/options");
            assertThat(opts.path("enable").asInt()).isEqualTo(1);
            assertThat(opts.path("policy_in").asText()).isEqualTo("DROP");
            assertThat(opts.path("policy_out").asText()).isEqualTo("DROP");
            List<String> rules = new ArrayList<>();
            client.getData(base + "/firewall/rules").forEach(r -> rules.add(r.path("type").asText() + " " + r.path("action").asText()
                    + " " + r.path("proto").asText() + " " + r.path("dport").asText() + " "
                    + (r.has("source") ? r.path("source").asText() : r.path("dest").asText())));
            assertThat(rules).containsExactlyInAnyOrder(
                    "in ACCEPT tcp 22 10.0.0.0/8",
                    "in ACCEPT tcp 22 2001:db8::/32",
                    "in ACCEPT icmp  0.0.0.0/0",
                    "out ACCEPT   0.0.0.0/0");
            // Replacing, not appending.
            assertThat(provider.applyFirewall(vmid, new WorkloadFirewall(List.of())).await(Duration.ofMinutes(1)))
                    .isEqualTo(OperationStatus.SUCCEEDED);
            assertThat(client.getData(base + "/firewall/rules").size()).isZero();

            assertThat(provider.start(vmid).await(Duration.ofMinutes(2))).isEqualTo(OperationStatus.SUCCEEDED);
            assertThat(provider.getVirtualMachine(vmid).state()).isEqualTo(VirtualMachineState.RUNNING);
            assertThat(provider.stop(vmid).await(Duration.ofMinutes(2))).isEqualTo(OperationStatus.SUCCEEDED);
        } finally {
            String id = vmid != null ? vmid : provider.listVirtualMachines().stream()
                    .filter(v -> v.name().equals(name)).map(v -> v.id()).findFirst().orElse(null);
            if (id != null) {
                if (provider.getVirtualMachine(id).state() == VirtualMachineState.RUNNING) {
                    provider.stop(id).await(Duration.ofMinutes(2));
                }
                assertThat(provider.destroy(id).await(Duration.ofMinutes(2))).isEqualTo(OperationStatus.SUCCEEDED);
            }
            client.close();
        }
    }

    private static String nodeOf(ProxmoxApiClient client, String vmid) {
        for (JsonNode r : client.getData("/cluster/resources?type=vm")) {
            if (r.path("vmid").asText().equals(vmid)) {
                return r.path("node").asText();
            }
        }
        throw new IllegalStateException("VM " + vmid + " not found");
    }
}
