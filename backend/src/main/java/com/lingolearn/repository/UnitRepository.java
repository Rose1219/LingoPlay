package com.lingolearn.repository;

import com.lingolearn.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findByCourseIdOrderBySortOrderAsc(Long courseId);
}