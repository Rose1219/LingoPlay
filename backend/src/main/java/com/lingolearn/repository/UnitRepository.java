package com.lingolearn.repository;

import com.lingolearn.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByCourseIdOrderBySortOrderAsc(Long courseId);

    /** 增量同步：按课程+标题定位已有单元 */
    Optional<Unit> findByCourseIdAndTitle(Long courseId, String title);
}