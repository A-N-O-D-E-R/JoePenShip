package io.virtualization.sdk.core.image;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageQueryTest {

    @Test
    void emptyQueryHasNoFilters() {
        ImageQuery query = ImageQuery.builder().build();

        assertThat(query.name()).isEmpty();
        assertThat(query.distribution()).isEmpty();
        assertThat(query.type()).isEmpty();
    }

    @Test
    void builderPopulatesGivenFields() {
        ImageQuery query = ImageQuery.builder()
                .distribution("ubuntu")
                .architecture("x86_64")
                .type(ImageType.CONTAINER)
                .remote("images")
                .provider("incus")
                .build();

        assertThat(query.distribution()).contains("ubuntu");
        assertThat(query.architecture()).contains("x86_64");
        assertThat(query.type()).contains(ImageType.CONTAINER);
        assertThat(query.remote()).contains("images");
        assertThat(query.provider()).contains("incus");
        assertThat(query.alias()).isEmpty();
    }
}
