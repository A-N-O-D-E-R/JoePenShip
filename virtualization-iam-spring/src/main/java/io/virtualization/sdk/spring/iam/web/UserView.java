package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.User;

import java.time.Instant;
import java.util.List;

/** {@link User} minus {@code passwordHash} — never serialize a password hash over REST. */
public record UserView(String id, String username, List<String> roles, List<String> groups, boolean enabled, Instant createdAt) {

    public static UserView from(User user) {
        return new UserView(
                user.id().value(), user.username(), user.roles().stream().map(RoleId::value).toList(),
                user.groups().stream().map(g -> g.value()).toList(), user.enabled(), user.createdAt());
    }
}
