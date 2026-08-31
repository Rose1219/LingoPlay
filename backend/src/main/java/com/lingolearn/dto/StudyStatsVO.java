package com.lingolearn.dto;

import lombok.Data;

import java.util.List;

/** 学习统计总览 */
@Data
public class StudyStatsVO {

    /** 连续打卡天数 */
    private Integer streakDays;

    /** 今日学习分钟数 */
    private Integer todayMinutes;

    /** 累计学习分钟数 */
    private Integer totalMinutes;

    /** 累计学习单词数 */
    private Integer wordsLearned;

    /** 已掌握单词数 */
    private Integer masteredWords;

    /** 已完成课时数 */
    private Integer lessonsCompleted;

    /** 开始学习的课时数 */
    private Integer lessonsStarted;

    /** 最近 90 天热力图数据 */
    private List<HeatItem> heatmap;

    /** 各模块平均得分 */
    private List<TypeAccuracy> typeAccuracy;

    @Data
    public static class HeatItem {
        private String date;
        private Integer minutes;
    }

    @Data
    public static class TypeAccuracy {
        private String type;
        private Integer accuracy;
        private Integer count;
    }
}