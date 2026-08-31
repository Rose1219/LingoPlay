package com.lingolearn.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingolearn.common.BusinessException;
import com.lingolearn.dto.AchievementVO;
import com.lingolearn.dto.SubmitRequest;
import com.lingolearn.dto.WordAnswerItem;
import com.lingolearn.entity.Language;
import com.lingolearn.entity.Lesson;
import com.lingolearn.entity.StudyRecord;
import com.lingolearn.entity.User;
import com.lingolearn.entity.UserWord;
import com.lingolearn.repository.LanguageRepository;
import com.lingolearn.repository.LessonRepository;
import com.lingolearn.repository.StudyRecordRepository;
import com.lingolearn.repository.UserRepository;
import com.lingolearn.repository.UserWordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** 游戏服务：每日单词、单词闯关等游戏化内容 */
@Service
public class GameService {

    private final LanguageRepository languageRepository;
    private final LessonRepository lessonRepository;
    private final UserRepository userRepository;
    private final UserWordRepository userWordRepository;
    private final StudyRecordRepository studyRecordRepository;
    private final AchievementService achievementService;
    private final ObjectMapper objectMapper;

    public GameService(LanguageRepository languageRepository, LessonRepository lessonRepository,
                       UserRepository userRepository, UserWordRepository userWordRepository,
                       StudyRecordRepository studyRecordRepository, AchievementService achievementService,
                       ObjectMapper objectMapper) {
        this.languageRepository = languageRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.userWordRepository = userWordRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.achievementService = achievementService;
        this.objectMapper = objectMapper;
    }

    /**
     * 每日单词：默认按日期固定，random=true 时随机抽取
     */
    @Transactional(readOnly = true)
    public Map<String, Object> dailyWord(Long userId, String langParam, boolean random) {
        Language lang = resolveLanguage(userId, langParam);
        List<Lesson> wordLessons = lessonRepository
                .findByTypeAndUnitCourseLanguageCode(Lesson.TYPE_WORD, lang.getCode());
        if (wordLessons.isEmpty()) {
            throw new BusinessException(404, "该语种暂无单词内容");
        }
        LocalDate today = LocalDate.now();
        int daySeed = today.getDayOfYear() * 31 + today.getYear() % 100;
        int lessonIdx = random
                ? (int) (Math.random() * wordLessons.size())
                : Math.abs(daySeed) % wordLessons.size();
        Lesson lesson = wordLessons.get(lessonIdx);
        try {
            JsonNode root = objectMapper.readTree(lesson.getContentJson());
            JsonNode words = root.path("words");
            if (!words.isArray() || words.size() == 0) {
                throw new BusinessException(404, "该课时暂无单词内容");
            }
            int wordIdx = random
                    ? (int) (Math.random() * words.size())
                    : Math.abs(daySeed * 7 + lessonIdx) % words.size();
            JsonNode w = words.get(wordIdx);
            Map<String, Object> result = new HashMap<>();
            result.put("lessonId", lesson.getId());
            result.put("word", w.path("word").asText());
            result.put("phonetic", w.path("phonetic").asText(""));
            result.put("meaning", w.path("meaning").asText());
            result.put("example", w.path("example").asText(""));
            result.put("translation", w.path("translation").asText(""));
            result.put("languageCode", lang.getCode());
            result.put("languageName", lang.getNameCn());
            result.put("icon", lang.getIcon());
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "单词数据解析失败");
        }
    }

    private Language resolveLanguage(Long userId, String langParam) {
        if (langParam != null && !langParam.trim().isEmpty()) {
            return languageRepository.findByCode(langParam.trim())
                    .orElseGet(this::firstLanguage);
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getPreferredLanguages() != null
                && !user.getPreferredLanguages().trim().isEmpty()) {
            String first = user.getPreferredLanguages().split(",")[0].trim();
            Optional<Language> lang = languageRepository.findByCode(first);
            if (lang.isPresent()) {
                return lang.get();
            }
        }
        return firstLanguage();
    }

    private Language firstLanguage() {
        return languageRepository.findAllByOrderBySortOrderAsc().stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(500, "系统未初始化语种数据"));
    }

    /**
     * 单词闯关词库：汇总该语种全部单词课时的单词
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> quizWords(Long userId, String langParam) {
        Language lang = resolveLanguage(userId, langParam);
        List<Lesson> wordLessons = lessonRepository
                .findByTypeAndUnitCourseLanguageCode(Lesson.TYPE_WORD, lang.getCode());
        List<Map<String, String>> result = new ArrayList<>();
        for (Lesson lesson : wordLessons) {
            try {
                JsonNode root = objectMapper.readTree(lesson.getContentJson());
                for (JsonNode w : root.path("words")) {
                    Map<String, String> m = new HashMap<>();
                    m.put("word", w.path("word").asText());
                    m.put("meaning", w.path("meaning").asText());
                    m.put("phonetic", w.path("phonetic").asText(""));
                    result.add(m);
                }
            } catch (Exception ignored) {
                // 单个课时内容异常时跳过，不影响整体词库
            }
        }
        return result;
    }

    /**
     * 单词闯关成绩提交：更新单词掌握度、每日学习记录并结算成就
     */
    @Transactional
    public Map<String, Object> submitQuiz(Long userId, SubmitRequest req) {
        String langCode = req.getLanguageCode() == null || req.getLanguageCode().trim().isEmpty()
                ? "en" : req.getLanguageCode().trim();
        Language language = languageRepository.findByCode(langCode).orElse(null);

        int newWords = 0;
        if (language != null && req.getWords() != null) {
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

        Map<String, Object> result = new HashMap<>();
        result.put("saved", true);
        result.put("newAchievements", unlocked);
        return result;
    }
}