package io.virtualization.sdk.iam;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A resource:action grant, e.g. {@code "vps:create"} or {@code "domain:records:list"} (a nested
 * action, {@code "records:list"}, under resource {@code "domain"}). A validated free-form string
 * pair, not an enum — this module depends on nothing above {@code virtualization-core} and must
 * stay that way, so it can never enumerate every resource type a future module (Projects, Network,
 * IPAM, ...) might add. {@link Permissions} is the one place the actual fixed catalog of valid
 * strings lives.
 */
public record Permission(String resource, String action) {

    private static final Pattern SEGMENT = Pattern.compile("[a-z][a-z0-9-]*");
    private static final Pattern ACTION = Pattern.compile("[a-z][a-z0-9-]*(:[a-z][a-z0-9-]*)*");

    public Permission {
        Objects.requireNonNull(resource, "resource must not be null");
        Objects.requireNonNull(action, "action must not be null");
        if (!SEGMENT.matcher(resource).matches()) {
            throw new IllegalArgumentException("resource must match " + SEGMENT.pattern() + ", was '" + resource + "'");
        }
        if (!ACTION.matcher(action).matches()) {
            throw new IllegalArgumentException("action must match " + ACTION.pattern() + ", was '" + action + "'");
        }
    }

    public String asString() {
        return resource + ":" + action;
    }

    /** @throws IllegalArgumentException if {@code value} has no {@code ':'} or either side fails validation */
    public static Permission parse(String value) {
        Objects.requireNonNull(value, "value must not be null");
        int firstColon = value.indexOf(':');
        if (firstColon < 0) {
            throw new IllegalArgumentException("'" + value + "' is not a valid permission — expected 'resource:action'");
        }
        return new Permission(value.substring(0, firstColon), value.substring(firstColon + 1));
    }

    @Override
    public String toString() {
        return asString();
    }
}
