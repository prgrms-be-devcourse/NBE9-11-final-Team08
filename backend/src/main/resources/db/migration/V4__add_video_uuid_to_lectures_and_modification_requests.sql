ALTER TABLE lectures ADD COLUMN video_uuid VARCHAR(36);
ALTER TABLE lecture_modification_requests ADD COLUMN after_video_uuid VARCHAR(36);

CREATE INDEX idx_lectures_video_uuid ON lectures(video_uuid);
CREATE INDEX idx_lecture_mod_requests_after_video_uuid ON lecture_modification_requests(after_video_uuid);