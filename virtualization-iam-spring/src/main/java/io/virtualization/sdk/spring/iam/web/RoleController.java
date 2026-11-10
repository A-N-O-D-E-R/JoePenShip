package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.RoleManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/iam/roles} — over whichever {@link RoleManager} bean {@code virtualization.iam.enabled=true} activates. */
@RestController
@RequestMapping("/api/v1/iam/roles")
public class RoleController {

    private final RoleManager roleManager;

    public RoleController(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @GetMapping
    public List<RoleView> list() {
        return roleManager.list().stream().map(RoleView::from).toList();
    }

    @PostMapping
    public RoleView create(@RequestBody CreateRoleRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        return RoleView.from(roleManager.create(request.name()));
    }

    @GetMapping("/{id}")
    public RoleView get(@PathVariable String id) {
        return RoleView.from(roleManager.get(new RoleId(id)));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        roleManager.delete(new RoleId(id));
    }

    @PostMapping("/{id}/permissions/grant")
    public RoleView grantPermission(@PathVariable String id, @RequestBody PermissionRequestBody request) {
        Objects.requireNonNull(request.permission(), "'permission' is required");
        return RoleView.from(roleManager.grantPermission(new RoleId(id), Permission.parse(request.permission())));
    }

    @PostMapping("/{id}/permissions/revoke")
    public RoleView revokePermission(@PathVariable String id, @RequestBody PermissionRequestBody request) {
        Objects.requireNonNull(request.permission(), "'permission' is required");
        return RoleView.from(roleManager.revokePermission(new RoleId(id), Permission.parse(request.permission())));
    }
}
