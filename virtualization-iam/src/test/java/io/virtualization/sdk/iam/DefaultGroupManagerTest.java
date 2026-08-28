package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultGroupManagerTest {

    private UserRepository users;
    private DefaultGroupManager manager;
    private User user;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        manager = new DefaultGroupManager(new InMemoryGroupRepository(), users);
        user = new DefaultUserManager(users).create("alice", "s3cret!".toCharArray());
    }

    @Test
    void addMemberUpdatesBothTheGroupAndTheUser() {
        Group group = manager.create("ops-team");

        Group updated = manager.addMember(group.id(), user.id());

        assertThat(updated.members()).containsExactly(user.id());
        assertThat(users.findById(user.id()).orElseThrow().groups()).containsExactly(group.id());
    }

    @Test
    void removeMemberUpdatesBothTheGroupAndTheUser() {
        Group group = manager.create("ops-team");
        manager.addMember(group.id(), user.id());

        Group updated = manager.removeMember(group.id(), user.id());

        assertThat(updated.members()).isEmpty();
        assertThat(users.findById(user.id()).orElseThrow().groups()).isEmpty();
    }

    @Test
    void addMemberForAnUnknownUserStillUpdatesTheGroup() {
        Group group = manager.create("ops-team");

        Group updated = manager.addMember(group.id(), UserId.generate());

        assertThat(updated.members()).hasSize(1);
    }

    @Test
    void assignRoleThenRemoveRoleRoundTrips() {
        Group group = manager.create("ops-team");
        RoleId roleId = RoleId.generate();

        Group withRole = manager.assignRole(group.id(), roleId);
        assertThat(withRole.roles()).containsExactly(roleId);

        Group withoutRole = manager.removeRole(group.id(), roleId);
        assertThat(withoutRole.roles()).isEmpty();
    }

    @Test
    void getUnknownGroupThrowsResourceNotFound() {
        assertThatThrownBy(() -> manager.get(GroupId.generate())).isInstanceOf(ResourceNotFoundException.class);
    }
}
