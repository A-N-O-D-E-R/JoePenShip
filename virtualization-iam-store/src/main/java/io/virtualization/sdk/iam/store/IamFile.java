package io.virtualization.sdk.iam.store;

import io.virtualization.sdk.iam.ApiCredential;
import io.virtualization.sdk.iam.Group;
import io.virtualization.sdk.iam.Role;
import io.virtualization.sdk.iam.User;

import java.util.List;

/** The on-disk shape of {@link JsonFileIamStore}'s single JSON file. */
record IamFile(List<User> users, List<Role> roles, List<Group> groups, List<ApiCredential> apiCredentials) {

    static final IamFile EMPTY = new IamFile(List.of(), List.of(), List.of(), List.of());
}
