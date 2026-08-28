package io.virtualization.sdk.iam;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** The only {@link AuthorizationService} implementation. */
public final class DefaultAuthorizationService implements AuthorizationService {

    private final RoleRepository roles;
    private final GroupRepository groups;

    public DefaultAuthorizationService(RoleRepository roles, GroupRepository groups) {
        this.roles = Objects.requireNonNull(roles, "roles must not be null");
        this.groups = Objects.requireNonNull(groups, "groups must not be null");
    }

    @Override
    public boolean hasPermission(User principal, Permission permission) {
        Objects.requireNonNull(principal, "principal must not be null");
        Objects.requireNonNull(permission, "permission must not be null");
        return principal.enabled() && effectivePermissions(principal).contains(permission);
    }

    @Override
    public Set<Permission> effectivePermissions(User principal) {
        Objects.requireNonNull(principal, "principal must not be null");
        Set<RoleId> roleIds = new HashSet<>(principal.roles());
        for (GroupId groupId : principal.groups()) {
            groups.findById(groupId).ifPresent(group -> roleIds.addAll(group.roles()));
        }
        Set<Permission> permissions = new HashSet<>();
        for (RoleId roleId : roleIds) {
            roles.findById(roleId).ifPresent(role -> permissions.addAll(role.permissions()));
        }
        return permissions;
    }
}
