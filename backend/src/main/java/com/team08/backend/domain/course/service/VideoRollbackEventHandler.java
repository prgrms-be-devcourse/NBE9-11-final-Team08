package com.team08.backend.domain.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoRollbackEventHandler {

    private final List<MediaEncodingService> mediaEncodingServices;

    @Async("videoEncodingExecutor")
    @EventListener
    public void cleanUpLeftoverVideos(VideoRollbackEvent event) {
        for (MediaEncodingService service : mediaEncodingServices) {
            if (service instanceof S3VideoEncodingService s3Service) {
                s3Service.deleteEncodedFolder(event.targetDirName(), event.lectureId());
            } else if (service instanceof LocalVideoEncodingService localService) {
                localService.deleteEncodedFolder(event.targetDirName());
            }
        }
    }
}