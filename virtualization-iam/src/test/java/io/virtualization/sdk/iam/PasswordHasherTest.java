package io.virtualization.sdk.iam;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHasherTest {

    @Test
    void verifyAcceptsTheCorrectPassword() {
        String encoded = PasswordHasher.hash("correct horse battery staple".toCharArray());

        assertThat(PasswordHasher.verify("correct horse battery staple".toCharArray(), encoded)).isTrue();
    }

    @Test
    void verifyRejectsTheWrongPassword() {
        String encoded = PasswordHasher.hash("correct horse battery staple".toCharArray());

        assertThat(PasswordHasher.verify("wrong password".toCharArray(), encoded)).isFalse();
    }

    @Test
    void sameThePasswordHashesDifferentlyEachTimeDueToRandomSalt() {
        String first = PasswordHasher.hash("same-password".toCharArray());
        String second = PasswordHasher.hash("same-password".toCharArray());

        assertThat(first).isNotEqualTo(second);
        assertThat(PasswordHasher.verify("same-password".toCharArray(), first)).isTrue();
        assertThat(PasswordHasher.verify("same-password".toCharArray(), second)).isTrue();
    }

    @Test
    void encodedFormatIsSelfDescribing() {
        String encoded = PasswordHasher.hash("password".toCharArray());

        assertThat(encoded).startsWith("pbkdf2-sha256$210000$");
    }

    @Test
    void verifyRejectsAGarbageEncodedValue() {
        assertThat(PasswordHasher.verify("password".toCharArray(), "not-a-valid-hash")).isFalse();
    }
}
