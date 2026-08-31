package com.lingolearn.repository;

import com.lingolearn.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByLanguageCodeOrderBySortOrderAsc(String languageCode);

    Optional<Course> findFirstByLanguageCodeAndLevelOrderBySortOrderAsc(String languageCode, String level);

    /** 增量同步：按语种+标题定位已有课程 */
    Optional<Course> findByLanguageIdAndTitle(Long languageId, String title);
}