package com.lingolearn.service;

import com.lingolearn.common.BusinessException;
import com.lingolearn.dto.*;
import com.lingolearn.entity.*;
import com.lingolearn.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 学习服务：开始学习、提交结果、单词记忆追踪 */
@Service
public class LearningService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final UserWordRepository userWordRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final AchievementService achievementService;
    private final UnitRepository unitRepository;

    public LearningService(LessonRepository lessonRepository, LessonProgressRepository progressRepository,
                           UserRepository userRepository, UserWordRepository userWordRepository,
                           StudyRecordRepository studyRecordRepository, AchievementService achievementService,
                           UnitRepository unitRepository) {
        this.lessonRepository = lessonRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.userWordRepository = userWordRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.achievementService = achievementService;
        this.unitRepository = unitRepository;
    }

    /** 开始学习：标记进行中 */
    @Transactional
    public void startLesson(Long userId, Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(404, "课时不存在"));
        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    LessonProgress p = new LessonProgress();
                    p.setUser(userRepository.getReferenceById(userId));
                    p.setLesson(lesson);
                    p.setBestScore(0);
                    p.setTotalAttempts(0);
                    return p;
                });
        if (!LessonProgress.STATUS_COMPLETED.equals(progress.getStatus())) {
            progress.setStatus(LessonProgress.STATUS_IN_PROGRESS);
        }
        progress.setLastStudiedAt(LocalDateTime.now());
        progressRepository.save(progress);
    }

    /** 提交学习结果 */
    @Transactional
    public SubmitResponse submitLesson(Long userId, Long lessonId, SubmitRequest req) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new BusinessException(404, "课时不存在"));
        LessonProgress progress = progressRepository.findByUserIdAndLessonId(userId, lessonId)
                .orElseGet(() -> {
                    LessonProgress p = new LessonProgress();
                    p.setUser(userRepository.getReferenceById(userId));
                    p.setLesson(lesson);
                    p.setBestScore(0);
                    p.setTotalAttempts(0);
                    return p;
                });
        progress.setTotalAttempts(progress.getTotalAttempts() + 1);
        progress.setLastStudiedAt(LocalDateTime.now());
        int score = req.getScore() == null ? 0 : Math.min(100, Math.max(0, req.getScore()));
        if (progress.getBestScore() == null || score > progress.getBestScore()) {
            progress.setBestScore(score);
        }
        // 得分及格即视为完成
        boolean completed = score >= 60;
        if (completed) {
            progress.setStatus(LessonProgress.STATUS_COMPLETED);
        } else {
            progress.setStatus(LessonProgress.STATUS_IN_PROGRESS);
        }
        progressRepository.save(progress);

        // 单词记忆追踪
        int newWords = 0;
        Language language = lesson.getUnit().getCourse().getLanguage();
        if (req.getWords() != null) {
            for (WordAnswerItem item : req.getWords()) {
                if (item.getWord() == null || item.getWord().trim().isEmpty()) {
                    continue;
                }
                UserWord uw = userWordRepository.findByUserIdAndLanguageIdAndWord(
                                userId, language.getId(), item.getWord().trim())
                        .orElseGet(() -> {
                            UserWord w = new UserWord();
                            w.setUser(userRepository.getReferenceById(userId));
                            w.setLanguage(language);
                            w.setWord(item.getWord().trim());
                            w.setMastery(0);
                            w.setReviewCount(0);
                            w.setCorrectStreak(0);
                            return w;
                        });
                boolean firstTime = uw.getReviewCount() == 0;
                uw.setMeaning(item.getMeaning());
                uw.setReviewCount(uw.getReviewCount() + 1);
                if (Boolean.TRUE.equals(item.getCorrect())) {
                    uw.setCorrectStreak(uw.getCorrectStreak() + 1);
                    uw.setMastery(Math.min(4, uw.getMastery() + 1));
                } else {
                    uw.setCorrectStreak(0);
                    uw.setMastery(Math.max(0, uw.getMastery() - 1));
                }
                uw.setLastReviewedAt(LocalDateTime.now());
                userWordRepository.save(uw);
                if (firstTime) {
                    newWords++;
                }
            }
        }

        // 每日学习记录
        LocalDate today = LocalDate.now();
        StudyRecord record = studyRecordRepository.findByUserIdAndStudyDate(userId, today)
                .orElseGet(() -> {
                    StudyRecord r = new StudyRecord();
                    r.setUser(userRepository.getReferenceById(userId));
                    r.setStudyDate(today);
                    return r;
                });
        record.setMinutes(record.getMinutes() + Math.max(1, req.getMinutes() == null ? 1 : req.getMinutes()));
        record.setWordsLearned(record.getWordsLearned() + newWords);
        record.setQuestionsAnswered(record.getQuestionsAnswered() + (req.getTotalCount() == null ? 0 : req.getTotalCount()));
        record.setCorrectAnswers(record.getCorrectAnswers() + (req.getCorrectCount() == null ? 0 : req.getCorrectCount()));
        studyRecordRepository.save(record);

        // 成就结算
        List<AchievementVO> unlocked = achievementService.evaluateAndUnlock(userId);

        // 课程内下一关（按单元与课时排序取当前课时的下一个）
        Lesson next = findNextLesson(lesson);

        SubmitResponse resp = new SubmitResponse();
        resp.setCompleted(completed);
        resp.setScore(score);
        resp.setNewAchievements(unlocked);
        resp.setHasNextLesson(next != null);
        if (next != null) {
            resp.setNextLessonId(next.getId());
            resp.setNextLessonTitle(next.getTitle());
        }
        return resp;
    }

    /** 找同课程内当前课时的下一个课时（单元排序 -> 课时排序） */
    private Lesson findNextLesson(Lesson current) {
        Long courseId = current.getUnit().getCourse().getId();
        boolean afterCurrent = false;
        for (Unit unit : unitRepository.findByCourseIdOrderBySortOrderAsc(courseId)) {
            for (Lesson l : lessonRepository.findByUnitIdOrderBySortOrderAsc(unit.getId())) {
                if (afterCurrent) {
                    return l;
                }
                if (l.getId().equals(current.getId())) {
                    afterCurrent = true;
                }
            }
        }
        return null;
    }
}