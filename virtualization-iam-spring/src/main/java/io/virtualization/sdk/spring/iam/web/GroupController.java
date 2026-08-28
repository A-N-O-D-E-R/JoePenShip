package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.GroupManager;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.UserId;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/iam/groups} — over whichever {@link GroupManager} bean {@code virtualization.iam.enabled=true} activates. */
@RestController
@RequestMapping("/api/v1/iam/groups")
public class GroupController {

    private final GroupManager groupManager;

    public GroupController(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @GetMapping
    public List<GroupView> list() {
        return groupManager.list().stream().map(GroupView::from).toList();
    }

    @PostMapping
    public GroupView create(@RequestBody CreateGroupRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        return GroupView.from(groupManager.create(request.name()));
    }

    @GetMapping("/{id}")
    public GroupView get(@PathVariable String id) {
        return GroupView.from(groupManager.get(new GroupId(id)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        groupManager.delete(new GroupId(id));
    }

    @PostMapping("/{id}/members/{userId}")
    public GroupView addMember(@PathVariable String id, @PathVariable String userId) {
        return GroupView.from(groupManager.addMember(new GroupId(id), new UserId(userId)));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public GroupView removeMember(@PathVariable String id, @PathVariable String userId) {
        return GroupView.from(groupManager.removeMember(new GroupId(id), new UserId(userId)));
    }

    @PostMapping("/{id}/roles/{roleId}")
    public GroupView assignRole(@PathVariable String id, @PathVariable String roleId) {
        return GroupView.from(groupManager.assignRole(new GroupId(id), new RoleId(roleId)));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public GroupView removeRole(@PathVariable String id, @PathVariable String roleId) {
        return GroupView.from(groupManager.removeRole(new GroupId(id), new RoleId(roleId)));
    }
}
