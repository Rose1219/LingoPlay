package com.lingolearn.repository;

import com.lingolearn.entity.LessonProgress;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    List<LessonProgress> findByUserIdAndLessonUnitCourseId(Long userId, Long courseId);

    List<LessonProgress> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    /** 按模块类型统计平均最佳得分，用于发现薄弱项 */
    @Query("select l.type, avg(lp.bestScore), count(lp) " +
            "from LessonProgress lp join lp.lesson l " +
            "where lp.user.id = :uid and lp.status = 'COMPLETED' " +
            "group by l.type")
    List<Object[]> aggregateTypeAccuracy(@Param("uid") Long userId);
}