package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface UserManager {

    /** @throws IllegalArgumentException if {@code username} is already taken */
    User create(String username, char[] password);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    User get(UserId id);

    List<User> list();

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    User setEnabled(UserId id, boolean enabled);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    User setPassword(UserId id, char[] newPassword);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    User assignRole(UserId id, RoleId roleId);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    User removeRole(UserId id, RoleId roleId);

    /** @throws AuthenticationException if the username is unknown, the account disabled, or the password doesn't match */
    User authenticate(String username, char[] password);
}
