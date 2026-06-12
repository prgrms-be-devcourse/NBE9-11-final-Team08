package com.team08.backend.domain.aifeedback.generator;

import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class OpenAiFeedbackGenerator implements AiFeedbackGenerator {

    private static final String SYSTEM_PROMPT = """
            당신은 한국어 학습 코치입니다.
            사용자가 작성한 스터디 활동을 존중하는 어조로 분석하고,
            반드시 한국어로 구체적이고 실행 가능한 피드백을 제공하세요.
            strengths, improvements, nextSteps는 각각 최소 한 개의 항목을 포함하세요.
            """;

    private final ChatClient chatClient;
    private final String modelName;
    private final String promptVersion;

    public OpenAiFeedbackGenerator(
            ChatClient.Builder chatClientBuilder,
            @Value("${app.ai.model:gpt-4.1-mini}") String modelName,
            @Value("${app.ai.prompt-version:v1}") String promptVersion
    ) {
        this.chatClient = chatClientBuilder.build();
        this.modelName = modelName;
        this.promptVersion = promptVersion;
    }

    @Override
    public StructuredFeedback generateKorean(String content) {
        return chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(user -> user.text("""
                        다음 스터디 활동 내용에 피드백을 제공하세요.

                        <activity>
                        {content}
                        </activity>
                        """).param("content", content))
                .call()
                .entity(StructuredFeedback.class);
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
