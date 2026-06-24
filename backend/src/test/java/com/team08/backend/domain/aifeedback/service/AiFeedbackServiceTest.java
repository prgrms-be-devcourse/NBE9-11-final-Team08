package com.team08.backend.domain.aifeedback.service;

import com.team08.backend.domain.aifeedback.dto.AiFeedbackResponse;
import com.team08.backend.domain.aifeedback.dto.StructuredFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedback;
import com.team08.backend.domain.aifeedback.entity.AiFeedbackStatus;
import com.team08.backend.domain.aifeedback.generator.AiFeedbackGenerator;
import com.team08.backend.domain.aifeedback.repository.AiFeedbackRepository;
import com.team08.backend.domain.study.access.StudyAccessAuthorizer;
import com.team08.backend.domain.study.access.StudyAction;
import com.team08.backend.domain.studyactivity.entity.StudyActivity;
import com.team08.backend.domain.studyactivity.repository.StudyActivityRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiFeedbackServiceTest {

    private static final Long STUDY_ID = 10L;
    private static final Long ACTIVITY_ID = 100L;
    private static final Long AUTHOR_ID = 1L;
    private static final String CONTENT = "AI 피드백을 요청할 스터디 활동 내용입니다.";

    @Mock
    private StudyActivityRepository studyActivityRepository;

    @Mock
    private AiFeedbackRepository aiFeedbackRepository;

    @Mock
    private AiFeedbackGenerator aiFeedbackGenerator;

    @Mock
    private StudyAccessAuthorizer studyAccessAuthorizer;

    @InjectMocks
    private AiFeedbackService aiFeedbackService;

    @Test
    void 작성자가_구조화된_한국어_AI_피드백을_생성한다() {
        StructuredFeedback generated = structuredFeedback();
        givenActivity();
        given(aiFeedbackRepository.findByStudyActivityIdForUpdate(ACTIVITY_ID))
                .willReturn(Optional.empty());
        given(aiFeedbackGenerator.modelName()).willReturn("gpt-4.1-mini");
        given(aiFeedbackGenerator.promptVersion()).willReturn("v1");
        given(aiFeedbackGenerator.generateKorean(CONTENT)).willReturn(generated);
        given(aiFeedbackRepository.save(org.mockito.ArgumentMatchers.any(AiFeedback.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        AiFeedbackResponse response =
                aiFeedbackService.generate(STUDY_ID, ACTIVITY_ID, AUTHOR_ID);

        assertThat(response.status()).isEqualTo(AiFeedbackStatus.COMPLETED);
        assertThat(response.result()).isEqualTo(generated);
        verify(studyAccessAuthorizer)
                .authorizeByStudyId(STUDY_ID, AUTHOR_ID, StudyAction.WRITE_STUDY_CONTENT);
    }

    @Test
    void 작성자가_아니면_피드백을_생성할_수_없다() {
        Long memberId = 2L;
        givenActivity();

        assertError(
                () -> aiFeedbackService.generate(STUDY_ID, ACTIVITY_ID, memberId),
                ErrorCode.STUDY_ACTIVITY_ACCESS_DENIED
        );
        then(aiFeedbackGenerator).shouldHaveNoInteractions();
    }

    @Test
    void 완료된_피드백과_활동_스냅샷이_같으면_AI를_다시_호출하지_않는다() {
        AiFeedback completed = completedFeedback(CONTENT);
        givenActivity();
        given(aiFeedbackRepository.findByStudyActivityIdForUpdate(ACTIVITY_ID))
                .willReturn(Optional.of(completed));

        AiFeedbackResponse response =
                aiFeedbackService.generate(STUDY_ID, ACTIVITY_ID, AUTHOR_ID);

        assertThat(response.status()).isEqualTo(AiFeedbackStatus.COMPLETED);
        then(aiFeedbackGenerator).shouldHaveNoInteractions();
    }

    @Test
    void FAILED_피드백은_명시적으로_재요청할_수_있다() {
        AiFeedback failed = AiFeedback.startProcessing(
                AUTHOR_ID, STUDY_ID, ACTIVITY_ID, CONTENT, "stub", "v1"
        );
        failed.fail();
        givenRetryableFeedback(failed);

        aiFeedbackService.generate(STUDY_ID, ACTIVITY_ID, AUTHOR_ID);

        then(aiFeedbackGenerator).should().generateKorean(CONTENT);
        assertThat(failed.getStatus()).isEqualTo(AiFeedbackStatus.COMPLETED);
    }

    @Test
    void STALE_피드백은_현재_활동_내용으로_재요청할_수_있다() {
        AiFeedback stale = completedFeedback("수정 전 활동 내용입니다.");
        stale.markStale();
        givenRetryableFeedback(stale);

        aiFeedbackService.generate(STUDY_ID, ACTIVITY_ID, AUTHOR_ID);

        then(aiFeedbackGenerator).should().generateKorean(CONTENT);
        assertThat(stale.getActivityContentSnapshot()).isEqualTo(CONTENT);
    }

    @Test
    void PROCESSING_피드백이_있으면_동시_요청을_거부한다() {
        AiFeedback processing = AiFeedback.startProcessing(
                AUTHOR_ID, STUDY_ID, ACTIVITY_ID, CONTENT, "stub", "v1"
        );
        givenActivity();
        given(aiFeedbackRepository.findByStudyActivityIdForUpdate(ACTIVITY_ID))
                .willReturn(Optional.of(processing));

        assertError(
                () -> aiFeedbackService.generate(STUDY_ID, ACTIVITY_ID, AUTHOR_ID),
                ErrorCode.AI_FEEDBACK_GENERATION_IN_PROGRESS
        );
        then(aiFeedbackGenerator).shouldHaveNoInteractions();
    }

    @Test
    void AI_호출이_실패하면_FAILED로_저장하고_오류를_반환한다() {
        AtomicReference<AiFeedback> savedFeedback = new AtomicReference<>();
        givenActivity();
        given(aiFeedbackRepository.findByStudyActivityIdForUpdate(ACTIVITY_ID))
                .willReturn(Optional.empty());
        given(aiFeedbackGenerator.modelName()).willReturn("stub");
        given(aiFeedbackGenerator.promptVersion()).willReturn("v1");
        given(aiFeedbackRepository.save(org.mockito.ArgumentMatchers.any(AiFeedback.class)))
                .willAnswer(invocation -> {
                    AiFeedback feedback = invocation.getArgument(0);
                    savedFeedback.set(feedback);
                    return feedback;
                });
        given(aiFeedbackGenerator.generateKorean(CONTENT))
                .willThrow(new IllegalStateException("provider unavailable"));

        assertError(
                () -> aiFeedbackService.generate(STUDY_ID, ACTIVITY_ID, AUTHOR_ID),
                ErrorCode.AI_FEEDBACK_GENERATION_FAILED
        );
        assertThat(savedFeedback.get().getStatus()).isEqualTo(AiFeedbackStatus.FAILED);
    }

    @Test
    void 생성_도중_활동_내용이_변경되면_결과를_STALE로_완료한다() {
        String originalContent = "생성 시작 시점의 활동 내용입니다.";
        StudyActivity changingActivity = org.mockito.Mockito.mock(StudyActivity.class);
        given(changingActivity.getContent()).willReturn(originalContent, CONTENT);
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                ACTIVITY_ID, STUDY_ID
        )).willReturn(Optional.of(changingActivity));
        given(aiFeedbackRepository.findByStudyActivityIdForUpdate(ACTIVITY_ID))
                .willReturn(Optional.empty());
        given(aiFeedbackGenerator.modelName()).willReturn("stub");
        given(aiFeedbackGenerator.promptVersion()).willReturn("v1");
        given(aiFeedbackRepository.save(org.mockito.ArgumentMatchers.any(AiFeedback.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        given(aiFeedbackGenerator.generateKorean(originalContent)).willReturn(structuredFeedback());

        AiFeedbackResponse response =
                aiFeedbackService.generate(STUDY_ID, ACTIVITY_ID, AUTHOR_ID);

        assertThat(response.status()).isEqualTo(AiFeedbackStatus.STALE);
    }

    @Test
    void 기존_피드백을_조회한다() {
        AiFeedback completed = completedFeedback(CONTENT);
        given(aiFeedbackRepository.findByStudyActivityId(ACTIVITY_ID))
                .willReturn(Optional.of(completed));

        AiFeedbackResponse response = aiFeedbackService.get(STUDY_ID, ACTIVITY_ID, 2L);

        assertThat(response.status()).isEqualTo(AiFeedbackStatus.COMPLETED);
        verify(studyAccessAuthorizer)
                .authorizeByStudyId(STUDY_ID, 2L, StudyAction.VIEW_STUDY_CONTENT);
    }

    @Test
    void 피드백이_없으면_NOT_FOUND를_반환한다() {
        given(aiFeedbackRepository.findByStudyActivityId(ACTIVITY_ID))
                .willReturn(Optional.empty());

        assertError(
                () -> aiFeedbackService.get(STUDY_ID, ACTIVITY_ID, 2L),
                ErrorCode.AI_FEEDBACK_NOT_FOUND
        );
    }

    private void givenRetryableFeedback(AiFeedback feedback) {
        givenActivity();
        given(aiFeedbackRepository.findByStudyActivityIdForUpdate(ACTIVITY_ID))
                .willReturn(Optional.of(feedback));
        given(aiFeedbackGenerator.modelName()).willReturn("stub");
        given(aiFeedbackGenerator.promptVersion()).willReturn("v1");
        given(aiFeedbackGenerator.generateKorean(CONTENT)).willReturn(structuredFeedback());
    }

    private void givenActivity() {
        given(studyActivityRepository.findByIdAndStudyIdAndDeletedAtIsNull(
                ACTIVITY_ID, STUDY_ID
        )).willReturn(Optional.of(activity()));
    }

    private StudyActivity activity() {
        StudyActivity activity = StudyActivity.create(STUDY_ID, AUTHOR_ID, CONTENT);
        ReflectionTestUtils.setField(activity, "id", ACTIVITY_ID);
        return activity;
    }

    private AiFeedback completedFeedback(String snapshot) {
        AiFeedback feedback = AiFeedback.startProcessing(
                AUTHOR_ID, STUDY_ID, ACTIVITY_ID, snapshot, "stub", "v1"
        );
        feedback.complete("""
                {"summary":"요약","strengths":["강점"],"improvements":["개선점"],"nextSteps":["다음 단계"]}
                """);
        return feedback;
    }

    private StructuredFeedback structuredFeedback() {
        return new StructuredFeedback(
                "핵심 내용을 잘 정리했습니다.",
                List.of("학습 내용을 구체적으로 설명했습니다."),
                List.of("근거를 한 가지 더 추가해 보세요."),
                List.of("예제 코드를 직접 작성해 보세요.")
        );
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
