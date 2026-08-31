package com.lingolearn.repository;

import com.lingolearn.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findAllByOrderByIdAsc();
}