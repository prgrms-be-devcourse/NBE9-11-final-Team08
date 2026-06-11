package com.team08.backend.domain.chapter.entity;

import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "chapters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE chapters SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Chapter extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToMany(
            mappedBy = "chapter",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Lecture> lectures = new ArrayList<>();

    @Builder
    public Chapter(String title, int orderNo, Course course) {
        this.title = title;
        this.orderNo = orderNo;
        this.course = course;
    }

    public void addLecture(Lecture lecture) {
        lectures.add(lecture);
        lecture.assignChapter(this);
    }

    public void assignCourse(Course course) {
        this.course = course;
    }

    public void updateGeneralInfo(String title, int orderNo, List<CourseUpdateRequest.LectureUpdateRequest> lectureRequests) {
        this.title = title;
        this.orderNo = orderNo;
        updateLectures(lectureRequests);
    }

    private void updateLectures(List<CourseUpdateRequest.LectureUpdateRequest> lectureRequests) {
        Map<Long, Lecture> existingLectureMap = this.lectures.stream()
                .filter(l -> l.getId() != null)
                .collect(Collectors.toMap(Lecture::getId, l -> l));

        List<Lecture> updatedLectures = new ArrayList<>();

        for (CourseUpdateRequest.LectureUpdateRequest lectureReq : lectureRequests) {
            if (lectureReq.id() != null && existingLectureMap.containsKey(lectureReq.id())) {
                Lecture existingLecture = existingLectureMap.get(lectureReq.id());
                existingLecture.updateGeneralInfo(
                        lectureReq.title(),
                        existingLecture.getSummary(),
                        lectureReq.durationSeconds(),
                        lectureReq.orderNo(),
                        lectureReq.isFreePreview()
                );
                updatedLectures.add(existingLecture);
            } else {
                Lecture newLecture = Lecture.builder()
                        .title(lectureReq.title())
                        .m3u8Path("")
                        .durationSeconds(lectureReq.durationSeconds())
                        .orderNo(lectureReq.orderNo())
                        .isFreePreview(lectureReq.isFreePreview())
                        .chapter(this)
                        .build();
                updatedLectures.add(newLecture);
            }
        }

        this.lectures.clear();
        this.lectures.addAll(updatedLectures);
    }
}