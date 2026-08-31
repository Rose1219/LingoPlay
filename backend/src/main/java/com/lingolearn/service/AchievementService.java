package com.lingolearn.service;

import com.lingolearn.dto.AchievementVO;
import com.lingolearn.entity.*;
import com.lingolearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 成就系统：按学习行为自动结算并解锁 */
@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final LessonProgressRepository progressRepository;
    private final UserWordRepository userWordRepository;
    private final StudyStatsService studyStatsService;
    private final PostRepository postRepository;
    private final CourseRepository courseRepository;
    private final LanguageRepository languageRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;

    public AchievementService(AchievementRepository achievementRepository,
                              UserAchievementRepository userAchievementRepository,
                              LessonProgressRepository progressRepository,
                              UserWordRepository userWordRepository,
                              StudyStatsService studyStatsService,
                              PostRepository postRepository,
                              CourseRepository courseRepository,
                              LanguageRepository languageRepository,
                              LessonRepository lessonRepository,
                              UserRepository userRepository) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.progressRepository = progressRepository;
        this.userWordRepository = userWordRepository;
        this.studyStatsService = studyStatsService;
        this.postRepository = postRepository;
        this.courseRepository = courseRepository;
        this.languageRepository = languageRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
    }

    /** 结算用户成就，返回本次新解锁列表 */
    @Transactional
    public List<AchievementVO> evaluateAndUnlock(Long userId) {
        Map<String, Long> counters = new HashMap<>();
        counters.put(Achievement.TYPE_LESSONS, progressRepository.countByUserIdAndStatus(userId, "COMPLETED"));
        counters.put(Achievement.TYPE_WORDS, userWordRepository.countByUserIdAndMasteryGreaterThan(userId, 3));
        counters.put(Achievement.TYPE_STREAK, (long) studyStatsService.computeStreak(userId));
        counters.put(Achievement.TYPE_POSTS, postRepository.countByUserId(userId));
        counters.put(Achievement.TYPE_LIKES, countLikesReceived(userId));
        counters.put(Achievement.TYPE_COURSE_LEVEL, isAnyA1Completed(userId) ? 1L : 0L);

        Set<Long> unlockedIds = userAchievementRepository.findByUserId(userId).stream()
                .map(ua -> ua.getAchievement().getId())
                .collect(Collectors.toSet());

        List<AchievementVO> newlyUnlocked = new ArrayList<>();
        for (Achievement def : achievementRepository.findAllByOrderByIdAsc()) {
            if (unlockedIds.contains(def.getId())) {
                continue;
            }
            long value = counters.getOrDefault(def.getType(), 0L);
            long threshold = parseThreshold(def.getThreshold());
            if (Achievement.TYPE_COURSE_LEVEL.equals(def.getType())) {
                if (value >= 1 && def.getThreshold() != null && !def.getThreshold().isEmpty()) {
                    unlock(userId, def, newlyUnlocked);
                }
            } else if (value >= threshold) {
                unlock(userId, def, newlyUnlocked);
            }
        }
        return newlyUnlocked;
    }

    private void unlock(Long userId, Achievement def, List<AchievementVO> newlyUnlocked) {
        UserAchievement ua = new UserAchievement();
        ua.setUser(userRepository.getReferenceById(userId));
        ua.setAchievement(def);
        ua.setUnlockedAt(LocalDateTime.now());
        userAchievementRepository.save(ua);
        newlyUnlocked.add(toVO(def, LocalDateTime.now()));
    }

    private long countLikesReceived(Long userId) {
        long sum = 0;
        for (Post p : postRepository.findByUserIdOrderByCreatedAtDesc(userId)) {
            sum += p.getLikeCount() == null ? 0 : p.getLikeCount();
        }
        return sum;
    }

    private boolean isAnyA1Completed(Long userId) {
        for (Language lang : languageRepository.findAllByOrderBySortOrderAsc()) {
            Optional<Course> a1 = courseRepository
                    .findFirstByLanguageCodeAndLevelOrderBySortOrderAsc(lang.getCode(), "A1");
            if (!a1.isPresent()) {
                continue;
            }
            long total = lessonRepository.countByUnitCourseId(a1.get().getId());
            if (total == 0) {
                continue;
            }
            long completed = progressRepository.findByUserIdAndLessonUnitCourseId(userId, a1.get().getId())
                    .stream().filter(p -> LessonProgress.STATUS_COMPLETED.equals(p.getStatus())).count();
            if (completed >= total) {
                return true;
            }
        }
        return false;
    }

    private long parseThreshold(String threshold) {
        try {
            return Long.parseLong(threshold);
        } catch (Exception e) {
            return Long.MAX_VALUE;
        }
    }

    /** 全部成就 + 我的解锁状态 */
    @Transactional(readOnly = true)
    public List<AchievementVO> listForUser(Long userId) {
        Map<Long, LocalDateTime> unlockedAt = new HashMap<>();
        for (UserAchievement ua : userAchievementRepository.findByUserId(userId)) {
            unlockedAt.put(ua.getAchievement().getId(), ua.getUnlockedAt());
        }
        List<AchievementVO> result = new ArrayList<>();
        for (Achievement def : achievementRepository.findAllByOrderByIdAsc()) {
            LocalDateTime at = unlockedAt.get(def.getId());
            result.add(toVO(def, at));
        }
        return result;
    }

    private AchievementVO toVO(Achievement def, LocalDateTime unlockedAt) {
        AchievementVO vo = new AchievementVO();
        vo.setId(def.getId());
        vo.setCode(def.getCode());
        vo.setName(def.getName());
        vo.setDescription(def.getDescription());
        vo.setIcon(def.getIcon());
        vo.setType(def.getType());
        vo.setThreshold(def.getThreshold());
        vo.setUnlocked(unlockedAt != null);
        vo.setUnlockedAt(unlockedAt);
        return vo;
    }
}