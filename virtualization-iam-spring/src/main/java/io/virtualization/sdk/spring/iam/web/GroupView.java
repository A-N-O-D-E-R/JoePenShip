package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.Group;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.UserId;

import java.time.Instant;
import java.util.List;

public record GroupView(String id, String name, List<String> roles, List<String> members, Instant createdAt) {

    public static GroupView from(Group group) {
        return new GroupView(
                group.id().value(), group.name(), group.roles().stream().map(RoleId::value).toList(),
                group.members().stream().map(UserId::value).toList(), group.createdAt());
    }
}
