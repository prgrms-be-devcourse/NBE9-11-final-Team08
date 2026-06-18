package com.team08.backend.domain.chapter.repository;

import com.team08.backend.domain.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    //코스의 챕터 목록을 순서대로 조회 (강의 목록 fetch join)
    @Query("SELECT DISTINCT c FROM Chapter c LEFT JOIN FETCH c.lectures l WHERE c.course.id = :courseId ORDER BY c.orderNo ASC")
    List<Chapter> findByCourseIdWithLecturesOrderByOrderNo(@Param("courseId") Long courseId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Chapter c SET c.orderNo = :orderNo WHERE c.id = :id AND c.course.id = :courseId")
    void updateOrderNo(@Param("id") Long id, @Param("orderNo") Integer orderNo, @Param("courseId") Long courseId);
}