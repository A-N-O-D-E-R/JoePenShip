package io.virtualization.sdk.iam;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PermissionTest {

    @Test
    void parseSplitsOnTheFirstColon() {
        Permission permission = Permission.parse("domain:records:list");

        assertThat(permission.resource()).isEqualTo("domain");
        assertThat(permission.action()).isEqualTo("records:list");
        assertThat(permission.asString()).isEqualTo("domain:records:list");
    }

    @Test
    void parseRejectsAValueWithNoColon() {
        assertThatThrownBy(() -> Permission.parse("vpscreate")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAnUppercaseResource() {
        assertThatThrownBy(() -> new Permission("VPS", "create")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyCatalogEntryParsesAndRoundTrips() {
        for (String value : Permissions.ALL) {
            assertThat(Permission.parse(value).asString()).isEqualTo(value);
        }
    }
}
