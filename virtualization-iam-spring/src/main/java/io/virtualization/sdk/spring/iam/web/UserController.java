package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.UserId;
import io.virtualization.sdk.iam.UserManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/iam/users} — over whichever {@link UserManager} bean {@code virtualization.iam.enabled=true} activates. */
@RestController
@RequestMapping("/api/v1/iam/users")
public class UserController {

    private final UserManager userManager;

    public UserController(UserManager userManager) {
        this.userManager = userManager;
    }

    @GetMapping
    public List<UserView> list() {
        return userManager.list().stream().map(UserView::from).toList();
    }

    @PostMapping
    public UserView create(@RequestBody CreateUserRequestBody request) {
        Objects.requireNonNull(request.username(), "'username' is required");
        Objects.requireNonNull(request.password(), "'password' is required");
        return UserView.from(userManager.create(request.username(), request.password().toCharArray()));
    }

    @GetMapping("/{id}")
    public UserView get(@PathVariable String id) {
        return UserView.from(userManager.get(new UserId(id)));
    }

    @PostMapping("/{id}/enable")
    public UserView enable(@PathVariable String id) {
        return UserView.from(userManager.setEnabled(new UserId(id), true));
    }

    @PostMapping("/{id}/disable")
    public UserView disable(@PathVariable String id) {
        return UserView.from(userManager.setEnabled(new UserId(id), false));
    }

    @PostMapping("/{id}/password")
    public UserView setPassword(@PathVariable String id, @RequestBody SetPasswordRequestBody request) {
        Objects.requireNonNull(request.password(), "'password' is required");
        return UserView.from(userManager.setPassword(new UserId(id), request.password().toCharArray()));
    }

    @PutMapping("/{id}/roles/{roleId}")
    public UserView assignRole(@PathVariable String id, @PathVariable String roleId) {
        return UserView.from(userManager.assignRole(new UserId(id), new RoleId(roleId)));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    public UserView removeRole(@PathVariable String id, @PathVariable String roleId) {
        return UserView.from(userManager.removeRole(new UserId(id), new RoleId(roleId)));
    }
}
