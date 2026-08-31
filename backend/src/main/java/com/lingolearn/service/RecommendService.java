package com.lingolearn.service;

import com.lingolearn.dto.ContinueLessonVO;
import com.lingolearn.dto.RecommendVO;
import com.lingolearn.dto.ReviewWordVO;
import com.lingolearn.entity.*;
import com.lingolearn.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 个性化学习路径推荐 */
@Service
public class RecommendService {

    private static final int[] REVIEW_INTERVAL_DAYS = {0, 1, 3, 7};

    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final CourseRepository courseRepository;
    private final UnitRepository unitRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final UserWordRepository userWordRepository;

    public RecommendService(UserRepository userRepository, LanguageRepository languageRepository,
                            CourseRepository courseRepository, UnitRepository unitRepository,
                            LessonRepository lessonRepository, LessonProgressRepository progressRepository,
                            UserWordRepository userWordRepository) {
        this.userRepository = userRepository;
        this.languageRepository = languageRepository;
        this.courseRepository = courseRepository;
        this.unitRepository = unitRepository;
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.userWordRepository = userWordRepository;
    }

    @Transactional(readOnly = true)
    public RecommendVO recommend(Long userId) {
        RecommendVO vo = new RecommendVO();
        User user = userRepository.findById(userId).orElse(null);
        List<String> preferredCodes = preferredCodes(user);

        // 1. 继续学习：进行中的课时（最多 3 个）
        List<ContinueLessonVO> continueLessons = progressRepository
                .findByUserIdAndStatus(userId, LessonProgress.STATUS_IN_PROGRESS, PageRequest.of(0, 3))
                .stream()
                .map(this::toContinueVO)
                .collect(Collectors.toList());
        continueLessons.removeIf(Objects::isNull);

        // 2. 补齐：每门偏好语言的下一个未完成课时
        for (String code : preferredCodes) {
            if (continueLessons.size() >= 4) {
                break;
            }
            ContinueLessonVO next = findNextLesson(userId, code);
            if (next != null && continueLessons.stream().noneMatch(c -> c.getLessonId().equals(next.getLessonId()))) {
                continueLessons.add(next);
            }
        }
        vo.setContinueLessons(continueLessons);

        // 3. 待复习单词（按记忆间隔到期）
        List<ReviewWordVO> reviewWords = new ArrayList<>();
        List<UserWord> candidates = userWordRepository
                .findByUserIdAndMasteryLessThanOrderByLastReviewedAtAsc(userId, 4, PageRequest.of(0, 60));
        LocalDateTime now = LocalDateTime.now();
        for (UserWord uw : candidates) {
            int intervalDays = REVIEW_INTERVAL_DAYS[Math.min(uw.getMastery(), 3)];
            LocalDateTime due = uw.getLastReviewedAt() == null
                    ? now.minusDays(1) : uw.getLastReviewedAt().plusDays(intervalDays);
            if (!now.isBefore(due)) {
                ReviewWordVO rv = new ReviewWordVO();
                rv.setWord(uw.getWord());
                rv.setMeaning(uw.getMeaning());
                rv.setLanguageCode(uw.getLanguage().getCode());
                rv.setMastery(uw.getMastery());
                reviewWords.add(rv);
            }
            if (reviewWords.size() >= 10) {
                break;
            }
        }
        vo.setReviewWords(reviewWords);
        vo.setTodayReviewCount(reviewWords.size());

        // 4. 薄弱模块
        String weakType = null;
        Integer weakAccuracy = null;
        for (Object[] row : progressRepository.aggregateTypeAccuracy(userId)) {
            double avg = ((Number) row[1]).doubleValue();
            int count = ((Number) row[2]).intValue();
            if (count >= 2 && (weakAccuracy == null || avg < weakAccuracy)) {
                weakType = (String) row[0];
                weakAccuracy = (int) Math.round(avg);
            }
        }
        vo.setWeakType(weakType);
        vo.setWeakTypeAccuracy(weakAccuracy);

        // 5. 学习建议
        vo.setSuggestion(buildSuggestion(user, continueLessons, reviewWords, weakType));
        return vo;
    }

    private List<String> preferredCodes(User user) {
        List<String> codes = new ArrayList<>();
        if (user != null && user.getPreferredLanguages() != null
                && !user.getPreferredLanguages().trim().isEmpty()) {
            for (String c : user.getPreferredLanguages().split(",")) {
                if (!c.trim().isEmpty()) {
                    codes.add(c.trim());
                }
            }
        }
        if (codes.isEmpty()) {
            for (Language lang : languageRepository.findAllByOrderBySortOrderAsc()) {
                codes.add(lang.getCode());
            }
        }
        return codes.stream().distinct().collect(Collectors.toList());
    }

    /** 查找某语种下第一个未完成的课时 */
    private ContinueLessonVO findNextLesson(Long userId, String languageCode) {
        for (Course course : courseRepository.findByLanguageCodeOrderBySortOrderAsc(languageCode)) {
            Set<Long> completedIds = progressRepository
                    .findByUserIdAndLessonUnitCourseId(userId, course.getId()).stream()
                    .filter(p -> LessonProgress.STATUS_COMPLETED.equals(p.getStatus()))
                    .map(p -> p.getLesson().getId())
                    .collect(Collectors.toSet());
            if (completedIds.size() >= (course.getLessonCount() == null ? 0 : course.getLessonCount())) {
                continue;
            }
            for (Unit unit : unitRepository.findByCourseIdOrderBySortOrderAsc(course.getId())) {
                for (Lesson lesson : lessonRepository.findByUnitIdOrderBySortOrderAsc(unit.getId())) {
                    if (!completedIds.contains(lesson.getId())) {
                        ContinueLessonVO vo = new ContinueLessonVO();
                        vo.setLessonId(lesson.getId());
                        vo.setTitle(lesson.getTitle());
                        vo.setType(lesson.getType());
                        vo.setCourseTitle(course.getTitle());
                        vo.setUnitTitle(unit.getTitle());
                        vo.setLanguageCode(languageCode);
                        vo.setLanguageIcon(course.getLanguage().getIcon());
                        return vo;
                    }
                }
            }
        }
        return null;
    }

    private ContinueLessonVO toContinueVO(LessonProgress p) {
        try {
            Lesson lesson = p.getLesson();
            Unit unit = lesson.getUnit();
            Course course = unit.getCourse();
            ContinueLessonVO vo = new ContinueLessonVO();
            vo.setLessonId(lesson.getId());
            vo.setTitle(lesson.getTitle());
            vo.setType(lesson.getType());
            vo.setCourseTitle(course.getTitle());
            vo.setUnitTitle(unit.getTitle());
            vo.setLanguageCode(course.getLanguage().getCode());
            vo.setLanguageIcon(course.getLanguage().getIcon());
            return vo;
        } catch (Exception e) {
            return null;
        }
    }

    private String buildSuggestion(User user, List<ContinueLessonVO> continueLessons,
                                   List<ReviewWordVO> reviewWords, String weakType) {
        StringBuilder sb = new StringBuilder();
        if (continueLessons.isEmpty()) {
            sb.append("欢迎开启学习之旅！选择一门课程，从入门课时开始吧。");
        } else {
            sb.append("今天继续学习「").append(continueLessons.get(0).getTitle()).append("」，保持学习节奏。");
        }
        if (!reviewWords.isEmpty()) {
            sb.append(" 有 ").append(reviewWords.size()).append(" 个单词到了复习时间，及时巩固记得更牢。");
        }
        if (weakType != null) {
            sb.append(" 你的").append(typeName(weakType)).append("模块还需加强，建议针对性练习。");
        }
        return sb.toString();
    }

    private String typeName(String type) {
        switch (type) {
            case "WORD":
                return "单词记忆";
            case "GRAMMAR":
                return "语法练习";
            case "SPEAK":
                return "口语跟读";
            case "LISTEN":
                return "听力训练";
            case "DIALOG":
                return "口语对话";
            default:
                return "学习";
        }
    }
}