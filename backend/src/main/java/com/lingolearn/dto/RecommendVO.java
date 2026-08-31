package com.lingolearn.dto;

import lombok.Data;

import java.util.List;

/** 个性化学习路径推荐 */
@Data
public class RecommendVO {

    /** 继续学习 / 下一步新课时 */
    private List<ContinueLessonVO> continueLessons;

    /** 待复习单词 */
    private List<ReviewWordVO> reviewWords;

    /** 薄弱模块类型（无完成记录时为 null） */
    private String weakType;

    /** 薄弱模块平均得分 */
    private Integer weakTypeAccuracy;

    /** 今日复习单词数 */
    private Integer todayReviewCount;

    /** 学习建议文案 */
    private String suggestion;
}