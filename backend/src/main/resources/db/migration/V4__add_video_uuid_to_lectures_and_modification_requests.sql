    ALTER TABLE lectures ADD COLUMN video_uuid VARCHAR(36);
    ALTER TABLE lecture_modification_requests ADD COLUMN after_video_uuid VARCHAR(36);