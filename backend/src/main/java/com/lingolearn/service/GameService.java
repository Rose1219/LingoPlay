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
    private final VipService vipService;
    private final ObjectMapper objectMapper;

    public GameService(LanguageRepository languageRepository, LessonRepository lessonRepository,
                       UserRepository userRepository, UserWordRepository userWordRepository,
                       StudyRecordRepository studyRecordRepository, AchievementService achievementService,
                       VipService vipService, ObjectMapper objectMapper) {
        this.languageRepository = languageRepository;
        this.lessonRepository = lessonRepository;
        this.userRepository = userRepository;
        this.userWordRepository = userWordRepository;
        this.studyRecordRepository = studyRecordRepository;
        this.achievementService = achievementService;
        this.vipService = vipService;
        this.objectMapper = objectMapper;
    }

    /** 词条候选（词 + 所属课程等级），level 为 A1/A2/B1/B2 */
    private static class WordEntry {
        final String word;
        final String meaning;
        final String phonetic;
        final String example;
        final String translation;
        final Long lessonId;
        final String level;

        WordEntry(JsonNode w, Long lessonId, String level) {
            this.word = w.path("word").asText();
            this.meaning = w.path("meaning").asText();
            this.phonetic = w.path("phonetic").asText("");
            this.example = w.path("example").asText("");
            this.translation = w.path("translation").asText("");
            this.lessonId = lessonId;
            this.level = level;
        }
    }

    /** CEFR 等级 → 难度分（A1=1 … B2=4），未知按 1 处理 */
    private static int levelDifficulty(String level) {
        switch (level == null ? "" : level) {
            case "A2": return 2;
            case "B1": return 3;
            case "B2": return 4;
            default: return 1;
        }
    }

    /** 按已入本词数决定难度上限：词量越多解锁越高的难度档位（更渐进的解锁曲线） */
    private static int difficultyCap(int learnedCount) {
        if (learnedCount < 10) return 1;   // 起步阶段专注 A1
        if (learnedCount < 25) return 2;   // 早期引入 A2
        if (learnedCount < 45) return 3;   // 中等阶段引入 B1
        return 4;                          // 45词以后开放所有级别（B2及以下）
    }

    /**
     * 每日单词（v1.0.4 重写）：
     * 1. 优先推送从未出现过的词（未入单词本），避免来回重复；
     * 2. 难度随已学词数递增（20/50/85 词分别解锁 A2/B1/B2）；
     * 3. 展示的词自动收入单词本（mastery=0），全部词汇随之沉淀；
     * 4. 非 random 时同一天稳定出同一个新词；random（换一个）随机换新词。
     */
    @Transactional
    public Map<String, Object> dailyWord(Long userId, String langParam, boolean random) {
        Language lang = resolveLanguage(userId, langParam);
        vipService.assertLanguageAccess(userId, lang);
        List<Lesson> wordLessons = lessonRepository
                .findByTypeAndUnitCourseLanguageCode(Lesson.TYPE_WORD, lang.getCode());
        if (wordLessons.isEmpty()) {
            throw new BusinessException(404, "该语种暂无单词内容");
        }

        // 1. 汇总全部词条并携带课程等级
        List<WordEntry> all = new ArrayList<>();
        try {
            for (Lesson lesson : wordLessons) {
                String level = lesson.getUnit().getCourse().getLevel();
                JsonNode words = objectMapper.readTree(lesson.getContentJson()).path("words");
                if (!words.isArray()) continue;
                for (JsonNode w : words) {
                    if (w.path("word").asText("").trim().isEmpty()) continue;
                    all.add(new WordEntry(w, lesson.getId(), level));
                }
            }
        } catch (Exception e) {
            throw new BusinessException(500, "单词数据解析失败");
        }
        if (all.isEmpty()) {
            throw new BusinessException(404, "该语种暂无单词内容");
        }

        // 2. 用户该语种已入本的词（出现过的词不再重复推）；游客无单词本，视为空
        List<UserWord> userWords = userId == null
                ? new ArrayList<>()
                : userWordRepository.findByUserIdAndLanguageId(userId, lang.getId());
        Set<String> learned = new HashSet<>();
        for (UserWord uw : userWords) {
            learned.add(uw.getWord());
        }

        // 词条索引（同词多课时时取第一个），供当天已推词直接复用
        Map<String, WordEntry> byWord = new HashMap<>();
        for (WordEntry e : all) {
            byWord.putIfAbsent(e.word, e);
        }

        // 3. 非 random 模式：当天已推送过的每日词直接复用，保证同一天刷新不跳词
        if (!random) {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            for (UserWord uw : userWords) {
                // reviewCount==0 表示仅由每日单词入本、尚未练习过
                if (uw.getReviewCount() == 0 && uw.getLastReviewedAt() != null
                        && uw.getLastReviewedAt().isAfter(todayStart)) {
                    WordEntry e = byWord.get(uw.getWord());
                    if (e != null) {
                        return buildDailyResult(e, lang, false, learned.size(), all.size());
                    }
                }
            }
        }

        // 4. 候选池：未出现过的词，且难度不超过当前解锁档位
        List<WordEntry> pool = new ArrayList<>();
        for (WordEntry e : all) {
            if (!learned.contains(e.word) && levelDifficulty(e.level) <= difficultyCap(learned.size())) {
                pool.add(e);
            }
        }
        // 档位内没有新词时放宽：允许全部未出现过的词
        if (pool.isEmpty()) {
            for (WordEntry e : all) {
                if (!learned.contains(e.word)) pool.add(e);
            }
        }
        // 词库全部出现过：进入复习模式，从全部词中选
        boolean reviewMode = pool.isEmpty();
        if (reviewMode) {
            pool.addAll(all);
        }

        // 5. 选择：random 随机；复习模式按日期种子稳定，避免刷新跳词
        WordEntry chosen;
        if (random) {
            chosen = pool.get((int) (Math.random() * pool.size()));
        } else {
            LocalDate today = LocalDate.now();
            int daySeed = today.getDayOfYear() * 31 + today.getYear() % 100;
            chosen = pool.get(Math.abs(daySeed) % pool.size());
        }

        // 6. 自动收入单词本：新词建档 mastery=0，全部词汇随之沉淀（游客只看不入本）
        boolean isNew = false;
        if (userId != null) {
            UserWord existing = userWordRepository
                    .findByUserIdAndLanguageIdAndWord(userId, lang.getId(), chosen.word)
                    .orElse(null);
            if (existing == null) {
                UserWord uw = new UserWord();
                uw.setUser(userRepository.getReferenceById(userId));
                uw.setLanguage(lang);
                uw.setWord(chosen.word);
                uw.setMeaning(chosen.meaning);
                uw.setMastery(0);
                uw.setReviewCount(0);
                uw.setCorrectStreak(0);
                uw.setLastReviewedAt(LocalDateTime.now());
                userWordRepository.save(uw);
                isNew = true;
            }
        }

        return buildDailyResult(chosen, lang, isNew, learned.size(), all.size());
    }

    /** 组装每日单词响应 */
    private Map<String, Object> buildDailyResult(WordEntry e, Language lang,
                                                 boolean isNew, int learnedCount, int totalCount) {
        Map<String, Object> result = new HashMap<>();
        result.put("lessonId", e.lessonId);
        result.put("word", e.word);
        result.put("phonetic", e.phonetic);
        result.put("meaning", e.meaning);
        result.put("example", e.example);
        result.put("translation", e.translation);
        result.put("languageCode", lang.getCode());
        result.put("languageName", lang.getNameCn());
        result.put("icon", lang.getIcon());
        result.put("level", e.level);
        result.put("isNew", isNew);
        result.put("learnedCount", learnedCount);
        result.put("totalCount", totalCount);
        return result;
    }

    private Language resolveLanguage(Long userId, String langParam) {
        if (langParam != null && !langParam.trim().isEmpty()) {
            return languageRepository.findByCode(langParam.trim())
                    .orElseGet(this::firstLanguage);
        }
        // 游客没有偏好语种，返回默认语种
        if (userId == null) {
            return firstLanguage();
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
        vipService.assertLanguageAccess(userId, lang);
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