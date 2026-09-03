package com.lingolearn.dto;

import lombok.Data;

import java.util.List;

/** 课时提交响应 */
@Data
public class SubmitResponse {

    /** 本轮是否将课时标记为完成 */
    private Boolean completed;

    /** 本轮得分（前端结算弹窗展示用） */
    private Integer score;

    /** 本次解锁的成就 */
    private List<AchievementVO> newAchievements;

    /** 课程内是否还有下一关（通关弹窗「进入下一关」用） */
    private Boolean hasNextLesson;

    /** 下一关课时 ID（hasNextLesson=true 时可用） */
    private Long nextLessonId;

    /** 下一关课时标题 */
    private String nextLessonTitle;
}