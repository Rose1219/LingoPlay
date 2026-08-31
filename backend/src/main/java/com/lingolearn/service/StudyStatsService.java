package com.lingolearn.service;

import com.lingolearn.dto.StudyStatsVO;
import com.lingolearn.entity.StudyRecord;
import com.lingolearn.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 学习统计服务：打卡、热力图、模块得分 */
@Service
public class StudyStatsService {

    private final StudyRecordRepository studyRecordRepository;
    private final LessonProgressRepository progressRepository;
    private final UserWordRepository userWordRepository;

    public StudyStatsService(StudyRecordRepository studyRecordRepository,
                             LessonProgressRepository progressRepository,
                             UserWordRepository userWordRepository) {
        this.studyRecordRepository = studyRecordRepository;
        this.progressRepository = progressRepository;
        this.userWordRepository = userWordRepository;
    }

    @Transactional(readOnly = true)
    public StudyStatsVO stats(Long userId) {
        StudyStatsVO vo = new StudyStatsVO();
        LocalDate today = LocalDate.now();

        // 最近 90 天热力图
        Map<LocalDate, Integer> minutesByDate = new HashMap<>();
        List<StudyRecord> recent = studyRecordRepository.findByUserIdAndStudyDateBetweenOrderByStudyDateAsc(
                userId, today.minusDays(89), today);
        for (StudyRecord r : recent) {
            minutesByDate.put(r.getStudyDate(), r.getMinutes());
        }
        List<StudyStatsVO.HeatItem> heatmap = new ArrayList<>();
        for (int i = 0; i < 90; i++) {
            LocalDate d = today.minusDays(89 - i);
            StudyStatsVO.HeatItem item = new StudyStatsVO.HeatItem();
            item.setDate(d.toString());
            item.setMinutes(minutesByDate.getOrDefault(d, 0));
            heatmap.add(item);
        }
        vo.setHeatmap(heatmap);

        // 连续打卡
        vo.setStreakDays(computeStreak(userId));
        vo.setTodayMinutes(minutesByDate.getOrDefault(today, 0));

        // 累计值
        List<Object[]> totals = studyRecordRepository.sumTotals(userId);
        Object[] t = totals.isEmpty() ? new Object[]{0L, 0L} : totals.get(0);
        vo.setTotalMinutes(((Number) t[0]).intValue());
        vo.setWordsLearned(((Number) t[1]).intValue());
        vo.setMasteredWords((int) userWordRepository.countByUserIdAndMasteryGreaterThan(userId, 3));
        vo.setLessonsStarted((int) progressRepository.countByUserId(userId));
        vo.setLessonsCompleted((int) progressRepository.countByUserIdAndStatus(userId, "COMPLETED"));

        // 各模块平均得分（仅统计已完成课时）
        List<StudyStatsVO.TypeAccuracy> accuracies = new ArrayList<>();
        for (Object[] row : progressRepository.aggregateTypeAccuracy(userId)) {
            StudyStatsVO.TypeAccuracy ta = new StudyStatsVO.TypeAccuracy();
            ta.setType((String) row[0]);
            ta.setAccuracy(row[1] == null ? null : ((Number) row[1]).intValue());
            ta.setCount(((Number) row[2]).intValue());
            accuracies.add(ta);
        }
        vo.setTypeAccuracy(accuracies);
        return vo;
    }

    /** 计算连续打卡天数：今天没学则从昨天开始算 */
    @Transactional(readOnly = true)
    public int computeStreak(Long userId) {
        List<StudyRecord> records = studyRecordRepository.findTop365ByUserIdOrderByStudyDateDesc(userId);
        if (records.isEmpty()) {
            return 0;
        }
        Map<LocalDate, Boolean> days = new HashMap<>();
        for (StudyRecord r : records) {
            days.put(r.getStudyDate(), true);
        }
        LocalDate cursor = LocalDate.now();
        if (!days.containsKey(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (days.containsKey(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}