package com.team08.backend.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileUrlFormatterImpl implements FileUrlFormatter {

    private final String distributionDomain;
    private final String bucket;
    private final boolean cloudFrontEnabled;
    private final String localVideoUrlPrefix = "http://localhost:8080/videos-local/";

    public FileUrlFormatterImpl(
            @Value("${cloud.aws.cloudfront.distribution-domain:}") String distributionDomain,
            @Value("${cloud.aws.s3.bucket:}") String bucket,
            @Value("${cloud.aws.cloudfront.enabled:true}") boolean cloudFrontEnabled) {
        this.distributionDomain = distributionDomain;
        this.bucket = bucket;
        this.cloudFrontEnabled = cloudFrontEnabled;
    }

    @Override
    public String formatVideoUrl(String m3u8Path) {
        if (m3u8Path == null || m3u8Path.isBlank()) {
            return "";
        }
        if (m3u8Path.startsWith("http")) {
            return m3u8Path;
        }
        if (cloudFrontEnabled && distributionDomain != null && !distributionDomain.isBlank()) {
            return "https://" + distributionDomain + "/" + m3u8Path;
        }
        return localVideoUrlPrefix + m3u8Path;
    }

    @Override
    public String formatThumbnailUrl(String thumbnailPath) {
        if (thumbnailPath == null || thumbnailPath.isBlank()) {
            return "";
        }
        if (thumbnailPath.startsWith("http")) {
            return thumbnailPath;
        }
        if (cloudFrontEnabled && distributionDomain != null && !distributionDomain.isBlank()) {
            return "https://" + distributionDomain + "/" + thumbnailPath;
        }
        if (bucket != null && !bucket.isBlank()) {
            return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + thumbnailPath;
        }
        return thumbnailPath;
    }
}
