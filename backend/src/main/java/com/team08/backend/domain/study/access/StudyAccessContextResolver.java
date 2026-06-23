package com.team08.backend.domain.study.access;

import com.team08.backend.domain.chapter.entity.Chapter;
import com.team08.backend.domain.chapter.repository.ChapterRepository;
import com.team08.backend.domain.enrollment.entity.EnrollmentStatus;
import com.team08.backend.domain.enrollment.repository.EnrollmentRepository;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.study.entity.Study;
import com.team08.backend.domain.study.entity.StudyStatus;
import com.team08.backend.domain.study.repository.StudyRepository;
import com.team08.backend.domain.studymember.entity.StudyMember;
import com.team08.backend.domain.studymember.entity.StudyMemberStatus;
import com.team08.backend.domain.studymember.repository.StudyMemberRepository;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StudyAccessContextResolver {

    private final StudyRepository studyRepository;
    private final ChapterRepository chapterRepository;
    private final LectureRepository lectureRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StudyAccessContext fromStudyId(Long studyId, Long userId) {
        Study study = studyRepository.findByIdWithCourse(studyId)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));

        return build(study, userId);
    }

    public StudyAccessContext fromCourseId(Long courseId, Long userId) {
        Study study = studyRepository.findByCourseIdAndStatusNotWithCourse(courseId, StudyStatus.INACTIVE)
                .orElseThrow(() -> new CustomException(ErrorCode.STUDY_NOT_FOUND));

        return build(study, userId);
    }

    public StudyAccessContext fromChapterId(Long chapterId, Long userId) {
        Chapter chapter = chapterRepository.findByIdWithCourse(chapterId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAPTER_NOT_FOUND));

        return fromCourseId(chapter.getCourse().getId(), userId);
    }

    public StudyAccessContext fromLectureId(Long lectureId, Long userId) {
        Lecture lecture = lectureRepository.findByIdWithChapterAndCourse(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        return fromCourseId(lecture.getChapter().getCourse().getId(), userId);
    }

    private StudyAccessContext build(Study study, Long userId) {
        Long studyId = study.getId();
        Long courseId = study.getCourse().getId();
        StudyMember studyMember = studyMemberRepository
                .findByStudyIdAndUserIdAndStatus(studyId, userId, StudyMemberStatus.ACTIVE)
                .orElse(null);
        boolean hasActiveEnrollment = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId, courseId, EnrollmentStatus.ACTIVE);

        return new StudyAccessContext(
                studyId,
                courseId,
                userId,
                study.getStatus(),
                hasActiveEnrollment,
                studyMember != null,
                studyMember != null ? studyMember.getRole() : null
        );
    }
}
