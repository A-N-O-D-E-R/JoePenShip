package io.virtualization.sdk.core.image;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ImageModelTest {

    @Test
    void imageIdRejectsBlank() {
        assertThat(new ImageId("sha256:abc").value()).isEqualTo("sha256:abc");

        assertThatIllegalArgumentException().isThrownBy(() -> new ImageId(""));
        assertThatNullPointerException().isThrownBy(() -> new ImageId(null));
    }

    @Test
    void imageReferenceRejectsBlankProviderOrIdentifier() {
        ImageReference reference = new ImageReference("incus", "images", "ubuntu/24.04");
        assertThat(reference.provider()).isEqualTo("incus");
        assertThat(reference.remote()).isEqualTo("images");
        assertThat(reference.identifier()).isEqualTo("ubuntu/24.04");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ImageReference("", "images", "ubuntu/24.04"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ImageReference("incus", "images", ""));
    }

    @Test
    void imageReferenceTwoArgConstructorLeavesRemoteNull() {
        ImageReference reference = new ImageReference("docker", "library/ubuntu:24.04");
        assertThat(reference.remote()).isNull();
    }

    @Test
    void imageConstructsAndRejectsInvalidFields() {
        ImageId id = new ImageId("sha256:abc");
        Instant now = Instant.now();
        Image image = new Image(
                id, "ubuntu/24.04", ImageType.CONTAINER, "x86_64", "ubuntu", "ubuntu", "24.04",
                512_000L, now, Map.of("incus.protocol", "1"));

        assertThat(image.id()).isEqualTo(id);
        assertThat(image.type()).isEqualTo(ImageType.CONTAINER);
        assertThat(image.metadata()).containsEntry("incus.protocol", "1");

        assertThatIllegalArgumentException().isThrownBy(() -> new Image(
                id, "", ImageType.CONTAINER, null, null, null, null, 0, null, Map.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> new Image(
                id, "ubuntu", ImageType.CONTAINER, null, null, null, null, -1, null, Map.of()));
        assertThatNullPointerException().isThrownBy(() -> new Image(
                null, "ubuntu", ImageType.CONTAINER, null, null, null, null, 0, null, Map.of()));
    }

    @Test
    void imageMetadataIsImmutable() {
        Image image = new Image(
                new ImageId("sha256:abc"), "ubuntu", ImageType.CONTAINER, null, null, null, null, 0,
                null, Map.of("k", "v"));

        assertThat(image.metadata()).isUnmodifiable();
    }

    @Test
    void imageAliasRejectsBlankName() {
        ImageId target = new ImageId("sha256:abc");
        ImageAlias alias = new ImageAlias("ubuntu/24.04", target);
        assertThat(alias.target()).isEqualTo(target);

        assertThatIllegalArgumentException().isThrownBy(() -> new ImageAlias("", target));
        assertThatNullPointerException().isThrownBy(() -> new ImageAlias("ubuntu/24.04", null));
    }
}
