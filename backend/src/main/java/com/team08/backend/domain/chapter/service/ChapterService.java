package com.team08.backend.domain.chapter.service;

import com.team08.backend.domain.chapter.dto.ChapterCreateRequest;
import com.team08.backend.domain.chapter.dto.ChapterWithLecturesResponse;
import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.repository.CourseRepository;
import com.team08.backend.domain.enrollment.service.EnrollmentAccessValidator;
import com.team08.backend.domain.lecture.dto.LectureEnterResponse;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.lecture.service.LectureService;
import com.team08.backend.domain.lectureprogress.repository.LectureProgressRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final LectureProgressRepository lectureProgressRepository;
    private final LectureService lectureService;
    private final EnrollmentAccessValidator enrollmentAccessValidator;


    /**
     * 챕터 생성
     */
    @Transactional
    public Long createChapter(Long courseId, ChapterCreateRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CustomException(ErrorCode.COURSE_NOT_FOUND));

        Chapter chapter = request.toEntity(course);
        course.addChapter(chapter);

        return chapterRepository.save(chapter).getId();
    }

    /**
     * 챕터 리스트 조회 — 코스 기준, 각 챕터에 속한 강의 목록 포함
     */
    @Transactional(readOnly = true)
    public List<ChapterWithLecturesResponse> getChaptersWithLectures(Long courseId) {
        List<Chapter> chapters = chapterRepository.findByCourseIdWithLecturesOrderByOrderNo(courseId);
        return chapters.stream()
                .map(ChapterWithLecturesResponse::from)
                .toList();
    }

    /**
     * 강좌 내 가장 최근 수강 강의 조회
     * 수강 이력이 없으면 null 반환
     */
    @Transactional(readOnly = true)
    public LectureEnterResponse getLastWatchedLecture(Long courseId, Long userId) {
        enrollmentAccessValidator.validateActiveEnrollment(userId, courseId);

        List<Long> lectureIds = lectureRepository.findIdsByCourseId(courseId);
        if (lectureIds.isEmpty()) {
            return null;
        }
        // TODO: (멘토님:강은혜) JPA 메소드가 병목 포인트,따로 벌크 처리같은게 필요함. 트랜잭션하나에 여러 쿼리가 함께쓰임-> 어떤 트랜잭션 전략으로 갈지 고민
        // get이라 무난해도 좋을거라 생각할 수 있지만 주요한 메타 데이터가 많이 관리 되는 곳이니 좀더 고민해서 구현
        // - 생각의 흐름 정리하는 법: 실시간으로 강좌/챕터/강의 별 시청중인 유저 수를 대시보드로 만들어 달라는 요구를 top down 으로 생각해보길 추천해주심
        // - 강좌수는 인프런같은 애들을 참고
        return lectureProgressRepository
                .findTopByUserIdAndLectureIdInOrderByUpdatedAtDesc(userId, lectureIds)
                .map(progress -> {
                    Lecture lecture = lectureRepository.findById(progress.getLectureId())
                            .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER));
                    return LectureEnterResponse.of(lecture, progress);
                })
                .orElse(null);
    }

    /**
     * 챕터 첫 강의 입장 — 해당 챕터의 orderNo 가장 낮은 강의로 입장한다.
     * 실제 입장(진행 행 보장 + 진행 정보 조립)과 수강권 검사는 강의 도메인의
     * {@link LectureService#enterLecture} 에 위임한다.
     */
    @Transactional
    public LectureEnterResponse enterFirstLecture(Long courseId, Long chapterId, Long userId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));
        // URL 정합성: 해당 챕터가 실제로 path 의 강좌 소속인지 확인한다. (수강권 검사는 enterLecture 가 담당)
        if (!chapter.getCourse().getId().equals(courseId)) {
            throw new CustomException(ErrorCode.CHAPTER_NOT_FOUND);
        }

        Lecture lecture = lectureRepository.findFirstByChapterIdOrderByOrderNoAsc(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND_IN_CHAPTER));

        return lectureService.enterLecture(lecture.getId(), userId);
    }

}