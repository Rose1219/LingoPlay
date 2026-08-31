package com.lingolearn.repository;

import com.lingolearn.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    List<UserAchievement> findByUserId(Long userId);

    List<UserAchievement> findByUserIdAndAchievementId(Long userId, Long achievementId);
}