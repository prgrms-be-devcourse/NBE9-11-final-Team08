package com.team08.backend.domain.lecturemodificationrequest.controller;

import com.team08.backend.domain.lecturemodificationrequest.dto.LectureModificationRequestCreateDto;
import com.team08.backend.domain.lecturemodificationrequest.service.LectureModificationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/lecture-modifications")
@RequiredArgsConstructor
@Tag(name = "강의 영상 수정 요청 API", description = "강사용 강의 영상 교체 요청 생성 API")
public class LectureModificationRequestController {

    private final LectureModificationRequestService modificationService;

    @PostMapping(consumes = {"multipart/form-data"})
    @Operation(summary = "강의 영상 수정 요청 생성", description = "강사가 기존 강의 영상 교체를 요청합니다. 요청 정보 JSON과 교체할 영상 파일을 multipart/form-data로 전송합니다.")
    public ResponseEntity<Void> createRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("request") @Valid LectureModificationRequestCreateDto requestDto,
            @RequestPart("video") MultipartFile videoFile
    ) {
        Long instructorId = Long.valueOf(userDetails.getUsername());

        modificationService.createRequest(requestDto, instructorId, videoFile);

        return ResponseEntity.ok().build();
    }
}
