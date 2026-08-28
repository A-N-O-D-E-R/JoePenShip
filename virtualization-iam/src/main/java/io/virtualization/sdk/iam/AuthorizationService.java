package io.virtualization.sdk.iam;

import java.util.Set;

/** Resolves a {@link User}'s effective permissions — direct role grants plus roles inherited through group membership. */
public interface AuthorizationService {
    boolean hasPermission(User principal, Permission permission);
    Set<Permission> effectivePermissions(User principal);
}
