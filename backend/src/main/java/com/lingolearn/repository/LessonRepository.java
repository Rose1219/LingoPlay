package com.lingolearn.repository;

import com.lingolearn.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByUnitIdOrderBySortOrderAsc(Long unitId);

    List<Lesson> findByTypeAndUnitCourseLanguageCode(String type, String languageCode);

    long countByUnitCourseId(Long courseId);
}