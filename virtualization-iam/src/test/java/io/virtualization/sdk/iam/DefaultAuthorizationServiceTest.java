package io.virtualization.sdk.iam;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAuthorizationServiceTest {

    private static final Permission VPS_CREATE = Permission.parse("vps:create");
    private static final Permission VPS_DESTROY = Permission.parse("vps:destroy");

    private RoleRepository roles;
    private GroupRepository groups;
    private DefaultAuthorizationService service;

    @BeforeEach
    void setUp() {
        roles = new InMemoryRoleRepository();
        groups = new InMemoryGroupRepository();
        service = new DefaultAuthorizationService(roles, groups);
    }

    @Test
    void grantsAPermissionHeldByADirectlyAssignedRole() {
        Role role = new Role(RoleId.generate(), "operators", Set.of(VPS_CREATE), Instant.now());
        roles.save(role);
        User user = user(Set.of(role.id()), Set.of());

        assertThat(service.hasPermission(user, VPS_CREATE)).isTrue();
        assertThat(service.hasPermission(user, VPS_DESTROY)).isFalse();
    }

    @Test
    void grantsAPermissionInheritedThroughGroupMembership() {
        Role role = new Role(RoleId.generate(), "operators", Set.of(VPS_DESTROY), Instant.now());
        roles.save(role);
        Group group = new Group(GroupId.generate(), "ops-team", Set.of(role.id()), Set.of(), Instant.now());
        groups.save(group);
        User user = user(Set.of(), Set.of(group.id()));

        assertThat(service.hasPermission(user, VPS_DESTROY)).isTrue();
    }

    @Test
    void aDisabledUserHasNoPermissionsEvenWithMatchingRoles() {
        Role role = new Role(RoleId.generate(), "operators", Set.of(VPS_CREATE), Instant.now());
        roles.save(role);
        User user = new User(UserId.generate(), "bob", "hash", Set.of(role.id()), Set.of(), false, Instant.now());

        assertThat(service.hasPermission(user, VPS_CREATE)).isFalse();
    }

    @Test
    void revokingARolesPermissionRemovesItFromEffectivePermissions() {
        Role role = new Role(RoleId.generate(), "operators", Set.of(VPS_CREATE), Instant.now());
        roles.save(role);
        User user = user(Set.of(role.id()), Set.of());
        assertThat(service.hasPermission(user, VPS_CREATE)).isTrue();

        roles.save(new Role(role.id(), role.name(), Set.of(), role.createdAt()));

        assertThat(service.hasPermission(user, VPS_CREATE)).isFalse();
    }

    private static User user(Set<RoleId> roles, Set<GroupId> groups) {
        return new User(UserId.generate(), "bob", "hash", roles, groups, true, Instant.now());
    }
}
