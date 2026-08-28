package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The only {@link GroupManager} implementation. Group membership is modeled on both sides
 * ({@link Group#members()} and {@link User#groups()}) — {@link #addMember}/{@link #removeMember}
 * keep both in sync (best-effort on the {@code User} side: a {@code userId} that doesn't resolve
 * to an existing user still updates the group's own membership list, matching this interface's
 * existing contract of validating only {@code id}, not {@code userId}). {@link
 * DefaultAuthorizationService} reads {@code User.groups()} to resolve inherited permissions, so an
 * out-of-sync {@code User} side would silently break group-inherited grants.
 */
public final class DefaultGroupManager implements GroupManager {

    private final GroupRepository repository;
    private final UserRepository users;

    public DefaultGroupManager(GroupRepository repository, UserRepository users) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.users = Objects.requireNonNull(users, "users must not be null");
    }

    @Override
    public Group create(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("group name '" + name + "' is already taken");
        }
        Group group = new Group(GroupId.generate(), name, Set.of(), Set.of(), Instant.now());
        repository.save(group);
        return group;
    }

    @Override
    public Group get(GroupId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No group with id '" + id.value() + "'"));
    }

    @Override
    public List<Group> list() {
        return repository.findAll();
    }

    @Override
    public void delete(GroupId id) {
        get(id);
        repository.delete(id);
    }

    @Override
    public Group addMember(GroupId id, UserId userId) {
        Group current = get(id);
        Set<UserId> members = new HashSet<>(current.members());
        members.add(userId);
        Group updated = new Group(current.id(), current.name(), current.roles(), members, current.createdAt());
        repository.save(updated);
        users.findById(userId).ifPresent(user -> {
            Set<GroupId> groups = new HashSet<>(user.groups());
            groups.add(id);
            users.save(new User(user.id(), user.username(), user.passwordHash(), user.roles(), groups, user.enabled(), user.createdAt()));
        });
        return updated;
    }

    @Override
    public Group removeMember(GroupId id, UserId userId) {
        Group current = get(id);
        Set<UserId> members = new HashSet<>(current.members());
        members.remove(userId);
        Group updated = new Group(current.id(), current.name(), current.roles(), members, current.createdAt());
        repository.save(updated);
        users.findById(userId).ifPresent(user -> {
            Set<GroupId> groups = new HashSet<>(user.groups());
            groups.remove(id);
            users.save(new User(user.id(), user.username(), user.passwordHash(), user.roles(), groups, user.enabled(), user.createdAt()));
        });
        return updated;
    }

    @Override
    public Group assignRole(GroupId id, RoleId roleId) {
        Group current = get(id);
        Set<RoleId> roles = new HashSet<>(current.roles());
        roles.add(roleId);
        Group updated = new Group(current.id(), current.name(), roles, current.members(), current.createdAt());
        repository.save(updated);
        return updated;
    }

    @Override
    public Group removeRole(GroupId id, RoleId roleId) {
        Group current = get(id);
        Set<RoleId> roles = new HashSet<>(current.roles());
        roles.remove(roleId);
        Group updated = new Group(current.id(), current.name(), roles, current.members(), current.createdAt());
        repository.save(updated);
        return updated;
    }
}
