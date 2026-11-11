package io.virtualization.sdk.core.firewall;

import java.util.List;
import java.util.Objects;

/**
 * The complete firewall of one workload, with cloud security-group semantics: stateful (replies
 * to allowed traffic pass), everything denied in both directions unless a rule allows it, rules
 * only allow. Applying it replaces whatever the provider had for that workload.
 */
public record WorkloadFirewall(List<Rule> rules) {

    public WorkloadFirewall {
        rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
    }

    public enum Direction {
        INBOUND,
        OUTBOUND
    }

    /**
     * Allows traffic.
     *
     * @param protocol {@code "tcp"}, {@code "udp"}, {@code "icmp"}, {@code "icmpv6"} or {@code null} for any
     * @param fromPort first port (tcp/udp) or ICMP type; {@code null} for all
     * @param toPort   last port (tcp/udp); ignored for ICMP; {@code null} when {@code fromPort} is
     * @param peers    source (inbound) or destination (outbound) CIDRs; must not be empty. A rule
     *                 whose peers are only known at runtime and currently empty should be omitted.
     * @param comment  free text kept with the rule where the provider supports it, or {@code null}
     */
    public record Rule(Direction direction, String protocol, Integer fromPort, Integer toPort, List<String> peers,
                       String comment) {

        public Rule {
            Objects.requireNonNull(direction, "direction must not be null");
            peers = List.copyOf(Objects.requireNonNull(peers, "peers must not be null"));
            if (peers.isEmpty()) {
                throw new IllegalArgumentException("peers must not be empty");
            }
            if (protocol != null && !List.of("tcp", "udp", "icmp", "icmpv6").contains(protocol)) {
                throw new IllegalArgumentException("Unsupported protocol '" + protocol + "'");
            }
            if (fromPort == null && toPort != null) {
                throw new IllegalArgumentException("toPort requires fromPort");
            }
            if ("tcp".equals(protocol) || "udp".equals(protocol)) {
                if (fromPort != null && (toPort == null || fromPort < 0 || toPort > 65535 || fromPort > toPort)) {
                    throw new IllegalArgumentException("Invalid port range " + fromPort + "-" + toPort);
                }
            }
        }
    }
}
