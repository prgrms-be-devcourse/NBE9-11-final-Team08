package com.team08.backend.domain.lecturemodificationrequest.service;

import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.domain.lecture.repository.LectureRepository;
import com.team08.backend.domain.course.service.MediaEncodingService;
import com.team08.backend.domain.lecturemodificationrequest.dto.LectureModificationRequestCreateDto;
import com.team08.backend.global.exception.CustomException;
import com.team08.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LectureModificationRequestService {

    private final LectureRepository lectureRepository;
    private final MediaEncodingService mediaEncodingService;

    @Transactional(readOnly = true)
    public void createRequest(LectureModificationRequestCreateDto dto, Long instructorId, MultipartFile videoFile) {
        Lecture lecture = lectureRepository.findById(dto.lectureId())
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));

        validateCourseOwnership(lecture, instructorId);

        String targetDirName = UUID.randomUUID().toString();

        mediaEncodingService.encodeModificationToHls(videoFile, targetDirName, dto.lectureId(), dto.description(), instructorId);
    }

    private void validateCourseOwnership(Lecture lecture, Long instructorId) {
        Long courseOwnerId = lecture.getChapter().getCourse().getInstructorId();
        if (!courseOwnerId.equals(instructorId)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED_COURSE_OWNER);
        }
    }
}