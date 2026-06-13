package com.team08.backend.domain.aifeedback.generator;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class AiFeedbackGeneratorProfileTest {

    @Test
    void dev와_test에서는_Stub_구현을_선택한다() {
        Profile profile = StubAiFeedbackGenerator.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactlyInAnyOrder("dev", "test");
    }

    @Test
    void prod에서는_OpenAI_구현을_선택한다() {
        Profile profile = OpenAiFeedbackGenerator.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("prod");
    }
}
