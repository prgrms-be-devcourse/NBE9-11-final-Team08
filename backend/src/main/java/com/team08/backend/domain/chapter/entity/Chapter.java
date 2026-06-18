package com.team08.backend.domain.chapter.entity;

import com.team08.backend.domain.course.dto.CourseUpdateRequest;
import com.team08.backend.domain.course.entity.Course;
import com.team08.backend.domain.lecture.entity.Lecture;
import com.team08.backend.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.*;
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
        Set<Long> requestIds = lectureRequests.stream()
                .map(CourseUpdateRequest.LectureUpdateRequest::id)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        this.lectures.removeIf(lecture -> !requestIds.contains(lecture.getId()));

        Map<Long, Lecture> existingLectureMap = this.lectures.stream()
                .collect(Collectors.toMap(Lecture::getId, lecture -> lecture));

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
            } else {
                this.addLecture(Lecture.create(
                        lectureReq.title(),
                        lectureReq.durationSeconds(),
                        lectureReq.orderNo(),
                        lectureReq.isFreePreview(),
                        this
                ));
            }
        }
    }

    public void updateOrderNo(int orderNo) {
        this.orderNo = orderNo;
    }
}