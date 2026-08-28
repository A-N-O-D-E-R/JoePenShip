package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.network.FirewallAction;
import io.virtualization.sdk.network.FirewallDirection;
import io.virtualization.sdk.network.FirewallProtocol;

public record CreateFirewallRuleRequestBody(
        FirewallDirection direction,
        FirewallProtocol protocol,
        Integer portRangeStart,
        Integer portRangeEnd,
        String cidr,
        FirewallAction action,
        int priority) {}
