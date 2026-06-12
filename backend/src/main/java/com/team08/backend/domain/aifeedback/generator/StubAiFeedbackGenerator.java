package com.team08.backend.domain.aifeedback.generator;

import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"dev", "test"})
public class StubAiFeedbackGenerator implements AiFeedbackGenerator {

    private final String modelName;
    private final String promptVersion;

    public StubAiFeedbackGenerator(
            @Value("${app.ai.model:stub}") String modelName,
            @Value("${app.ai.prompt-version:v1}") String promptVersion
    ) {
        this.modelName = modelName;
        this.promptVersion = promptVersion;
    }

    @Override
    public StructuredFeedback generateKorean(String content) {
        return new StructuredFeedback(
                "학습 활동의 핵심 내용을 명확하게 기록했습니다.",
                List.of("학습한 내용을 스스로 정리하고 공유한 점이 좋습니다."),
                List.of("배운 내용의 근거나 구체적인 예시를 조금 더 추가해 보세요."),
                List.of("다음 학습에서 적용할 작은 실천 항목을 하나 정해 보세요.")
        );
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public String promptVersion() {
        return promptVersion;
    }
}
