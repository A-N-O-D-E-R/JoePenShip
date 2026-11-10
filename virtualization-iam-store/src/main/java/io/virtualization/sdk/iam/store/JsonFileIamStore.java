package io.virtualization.sdk.iam.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.virtualization.sdk.iam.ApiCredential;
import io.virtualization.sdk.iam.ApiCredentialId;
import io.virtualization.sdk.iam.ApiCredentialRepository;
import io.virtualization.sdk.iam.Group;
import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.GroupRepository;
import io.virtualization.sdk.iam.Role;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.RoleRepository;
import io.virtualization.sdk.iam.User;
import io.virtualization.sdk.iam.UserId;
import io.virtualization.sdk.iam.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * A durable {@link UserRepository}/{@link RoleRepository}/{@link GroupRepository}/{@link
 * ApiCredentialRepository} backed by a single JSON file — the first repository implementation in
 * this codebase shipped as a real Spring default rather than a CLI-only convenience: losing every
 * account on an application restart is a real defect for IAM specifically, unlike it was
 * acceptable for VPS/DNS/certificate state.
 *
 * <p>One file, not four: these four aggregates cross-reference each other by id ({@code
 * User.roles}/{@code Group.roles} -> {@code RoleId}, {@code ApiCredential.ownerId} -> {@code
 * UserId}), so a single file keeps e.g. a role deletion's read-modify-write atomic instead of
 * racing across four separately-locked files. Same whole-file-read-modify-write pattern as {@code
 * JsonFileVpsRepository}/{@code JsonFileCertificateRepository} in {@code virtualization-cli} — all
 * four views below run their entire read-modify-write under this instance's own monitor, so a
 * mutation on any one aggregate can never interleave with another.
 *
 * <p>Only password/token *hashes* are ever stored, never a recoverable secret, so plain JSON is
 * fine — unlike a raw private key, a hash needs no field-level encryption to stay useless to a
 * reader who isn't already trying every password/token. A best-effort {@code rw-------}
 * permission is still set on every write, POSIX filesystems only.
 *
 * <p>ponytail: in-process lock only, no OS-level file locking — same accepted tradeoff as every
 * other {@code JsonFile*} repository in this codebase.
 */
public final class JsonFileIamStore {

    private static final Logger log = LoggerFactory.getLogger(JsonFileIamStore.class);

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Set<PosixFilePermission> OWNER_READ_WRITE = PosixFilePermissions.fromString("rw-------");

    private final Path file;
    private final UserRepository users = new UserRepositoryView();
    private final RoleRepository roles = new RoleRepositoryView();
    private final GroupRepository groups = new GroupRepositoryView();
    private final ApiCredentialRepository apiCredentials = new ApiCredentialRepositoryView();

    public JsonFileIamStore(Path file) {
        this.file = Objects.requireNonNull(file, "file must not be null");
    }

    public UserRepository users() {
        return users;
    }

    public RoleRepository roles() {
        return roles;
    }

    public GroupRepository groups() {
        return groups;
    }

    public ApiCredentialRepository apiCredentials() {
        return apiCredentials;
    }

    private IamFile readAll() {
        if (!Files.exists(file)) {
            return IamFile.EMPTY;
        }
        try {
            return MAPPER.readValue(file.toFile(), IamFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read IAM state file '" + file + "'", e);
        }
    }

    private void writeAll(IamFile state) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writeValue(file.toFile(), state);
            setOwnerOnlyPermissionsBestEffort();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write IAM state file '" + file + "'", e);
        }
    }

    private void setOwnerOnlyPermissionsBestEffort() {
        try {
            Files.setPosixFilePermissions(file, OWNER_READ_WRITE);
        } catch (UnsupportedOperationException e) {
            // non-POSIX filesystem (e.g. Windows) — ponytail: no ACL-based equivalent implemented.
        } catch (IOException e) {
            log.warn("Failed to restrict permissions on IAM state file '{}': {}", file, e.getMessage());
        }
    }

    private static <ID, T> Map<ID, T> indexById(List<T> items, Function<T, ID> idFn) {
        Map<ID, T> map = new LinkedHashMap<>();
        for (T item : items) {
            map.put(idFn.apply(item), item);
        }
        return map;
    }

    private final class UserRepositoryView implements UserRepository {

        @Override
        public void save(User user) {
            synchronized (JsonFileIamStore.this) {
                IamFile state = readAll();
                Map<UserId, User> byId = indexById(state.users(), User::id);
                byId.put(user.id(), user);
                writeAll(new IamFile(List.copyOf(byId.values()), state.roles(), state.groups(), state.apiCredentials()));
            }
        }

        @Override
        public Optional<User> findById(UserId id) {
            synchronized (JsonFileIamStore.this) {
                return readAll().users().stream().filter(u -> u.id().equals(id)).findFirst();
            }
        }

        @Override
        public Optional<User> findByUsername(String username) {
            synchronized (JsonFileIamStore.this) {
                return readAll().users().stream().filter(u -> u.username().equals(username)).findFirst();
            }
        }

        @Override
        public List<User> findAll() {
            synchronized (JsonFileIamStore.this) {
                return readAll().users();
            }
        }

        @Override
        public void delete(UserId id) {
            synchronized (JsonFileIamStore.this) {
                IamFile state = readAll();
                List<User> remaining = state.users().stream().filter(u -> !u.id().equals(id)).toList();
                writeAll(new IamFile(remaining, state.roles(), state.groups(), state.apiCredentials()));
            }
        }
    }

    private final class RoleRepositoryView implements RoleRepository {

        @Override
        public void save(Role role) {
            synchronized (JsonFileIamStore.this) {
                IamFile state = readAll();
                Map<RoleId, Role> byId = indexById(state.roles(), Role::id);
                byId.put(role.id(), role);
                writeAll(new IamFile(state.users(), List.copyOf(byId.values()), state.groups(), state.apiCredentials()));
            }
        }

        @Override
        public Optional<Role> findById(RoleId id) {
            synchronized (JsonFileIamStore.this) {
                return readAll().roles().stream().filter(r -> r.id().equals(id)).findFirst();
            }
        }

        @Override
        public Optional<Role> findByName(String name) {
            synchronized (JsonFileIamStore.this) {
                return readAll().roles().stream().filter(r -> r.name().equals(name)).findFirst();
            }
        }

        @Override
        public List<Role> findAll() {
            synchronized (JsonFileIamStore.this) {
                return readAll().roles();
            }
        }

        @Override
        public void delete(RoleId id) {
            synchronized (JsonFileIamStore.this) {
                IamFile state = readAll();
                List<Role> remaining = state.roles().stream().filter(r -> !r.id().equals(id)).toList();
                writeAll(new IamFile(state.users(), remaining, state.groups(), state.apiCredentials()));
            }
        }
    }

    private final class GroupRepositoryView implements GroupRepository {

        @Override
        public void save(Group group) {
            synchronized (JsonFileIamStore.this) {
                IamFile state = readAll();
                Map<GroupId, Group> byId = indexById(state.groups(), Group::id);
                byId.put(group.id(), group);
                writeAll(new IamFile(state.users(), state.roles(), List.copyOf(byId.values()), state.apiCredentials()));
            }
        }

        @Override
        public Optional<Group> findById(GroupId id) {
            synchronized (JsonFileIamStore.this) {
                return readAll().groups().stream().filter(g -> g.id().equals(id)).findFirst();
            }
        }

        @Override
        public Optional<Group> findByName(String name) {
            synchronized (JsonFileIamStore.this) {
                return readAll().groups().stream().filter(g -> g.name().equals(name)).findFirst();
            }
        }

        @Override
        public List<Group> findAll() {
            synchronized (JsonFileIamStore.this) {
                return readAll().groups();
            }
        }

        @Override
        public void delete(GroupId id) {
            synchronized (JsonFileIamStore.this) {
                IamFile state = readAll();
                List<Group> remaining = state.groups().stream().filter(g -> !g.id().equals(id)).toList();
                writeAll(new IamFile(state.users(), state.roles(), remaining, state.apiCredentials()));
            }
        }
    }

    private final class ApiCredentialRepositoryView implements ApiCredentialRepository {

        @Override
        public void save(ApiCredential credential) {
            synchronized (JsonFileIamStore.this) {
                IamFile state = readAll();
                Map<ApiCredentialId, ApiCredential> byId = indexById(state.apiCredentials(), ApiCredential::id);
                byId.put(credential.id(), credential);
                writeAll(new IamFile(state.users(), state.roles(), state.groups(), List.copyOf(byId.values())));
            }
        }

        @Override
        public Optional<ApiCredential> findById(ApiCredentialId id) {
            synchronized (JsonFileIamStore.this) {
                return readAll().apiCredentials().stream().filter(c -> c.id().equals(id)).findFirst();
            }
        }

        @Override
        public Optional<ApiCredential> findByTokenHash(String tokenHash) {
            synchronized (JsonFileIamStore.this) {
                return readAll().apiCredentials().stream().filter(c -> c.tokenHash().equals(tokenHash)).findFirst();
            }
        }

        @Override
        public List<ApiCredential> findByOwner(UserId ownerId) {
            synchronized (JsonFileIamStore.this) {
                return readAll().apiCredentials().stream().filter(c -> c.ownerId().equals(ownerId)).toList();
            }
        }

        @Override
        public List<ApiCredential> findAll() {
            synchronized (JsonFileIamStore.this) {
                return readAll().apiCredentials();
            }
        }

        @Override
        public void delete(ApiCredentialId id) {
            synchronized (JsonFileIamStore.this) {
                IamFile state = readAll();
                List<ApiCredential> remaining = state.apiCredentials().stream().filter(c -> !c.id().equals(id)).toList();
                writeAll(new IamFile(state.users(), state.roles(), state.groups(), remaining));
            }
        }
    }
}
