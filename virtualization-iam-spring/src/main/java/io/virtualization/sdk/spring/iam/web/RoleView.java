package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.Role;

import java.time.Instant;
import java.util.List;

public record RoleView(String id, String name, List<String> permissions, Instant createdAt) {

    public static RoleView from(Role role) {
        return new RoleView(role.id().value(), role.name(), role.permissions().stream().map(Permission::asString).toList(), role.createdAt());
    }
}
