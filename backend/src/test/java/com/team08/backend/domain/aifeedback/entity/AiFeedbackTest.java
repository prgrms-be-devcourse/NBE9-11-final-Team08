package com.team08.backend.domain.aifeedback.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiFeedbackTest {

    private static final String SNAPSHOT = "AI 피드백을 요청할 스터디 활동 내용입니다.";
    private static final String RESULT = """
            {
              "summary": "핵심 내용을 잘 정리했습니다.",
              "strengths": ["학습 내용을 구체적으로 설명했습니다."],
              "improvements": ["근거를 한 가지 더 추가해 보세요."],
              "nextSteps": ["예제 코드를 직접 작성해 보세요."]
            }
            """;

    @Test
    void 피드백_생성을_시작하면_활동_내용을_스냅샷으로_저장한다() {
        AiFeedback feedback = AiFeedback.startProcessing(
                1L, 10L, 100L, SNAPSHOT, "gpt-4.1-mini", "v1"
        );

        assertThat(feedback.getStatus()).isEqualTo(AiFeedbackStatus.PROCESSING);
        assertThat(feedback.getActivityContentSnapshot()).isEqualTo(SNAPSHOT);
    }

    @Test
    void 구조화된_결과로_피드백_생성을_완료한다() {
        AiFeedback feedback = feedbackInProgress();

        feedback.complete(RESULT);

        assertThat(feedback.getStatus()).isEqualTo(AiFeedbackStatus.COMPLETED);
        assertThat(feedback.getFeedback()).isEqualTo(RESULT);
    }

    @Test
    void AI_호출이_실패하면_FAILED로_전이한다() {
        AiFeedback feedback = feedbackInProgress();

        feedback.fail();

        assertThat(feedback.getStatus()).isEqualTo(AiFeedbackStatus.FAILED);
    }

    @Test
    void 완료된_피드백을_STALE로_바꿔도_기존_구조화_결과를_유지한다() {
        AiFeedback feedback = feedbackInProgress();
        feedback.complete(RESULT);

        feedback.markStale();

        assertThat(feedback.getStatus()).isEqualTo(AiFeedbackStatus.STALE);
        assertThat(feedback.getFeedback()).isEqualTo(RESULT);
    }

    @Test
    void FAILED와_STALE은_새_활동_스냅샷으로_재요청할_수_있다() {
        AiFeedback failed = feedbackInProgress();
        failed.fail();
        AiFeedback stale = feedbackInProgress();
        stale.complete(RESULT);
        stale.markStale();

        failed.restartProcessing("실패 후 수정한 활동 내용입니다.", "gpt-4.1-mini", "v1");
        stale.restartProcessing("완료 후 수정한 활동 내용입니다.", "gpt-4.1-mini", "v1");

        assertThat(failed.getStatus()).isEqualTo(AiFeedbackStatus.PROCESSING);
        assertThat(failed.getActivityContentSnapshot()).isEqualTo("실패 후 수정한 활동 내용입니다.");
        assertThat(stale.getStatus()).isEqualTo(AiFeedbackStatus.PROCESSING);
        assertThat(stale.getActivityContentSnapshot()).isEqualTo("완료 후 수정한 활동 내용입니다.");
    }

    private AiFeedback feedbackInProgress() {
        return AiFeedback.startProcessing(
                1L, 10L, 100L, SNAPSHOT, "gpt-4.1-mini", "v1"
        );
    }
}
