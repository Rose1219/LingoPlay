package com.lingolearn.dto;

import lombok.Data;

import java.util.List;

/** 课时提交结果 */
@Data
public class SubmitRequest {

    /** 本次学习时长（分钟） */
    private Integer minutes = 1;

    /** 得分 0-100 */
    private Integer score;

    private Integer correctCount = 0;

    private Integer totalCount = 0;

    /** 单词模块的作答结果（用于更新单词本） */
    private List<WordAnswerItem> words;

    /** 单词闯关游戏提交时的语种（课时提交时为空，按课时语种处理） */
    private String languageCode;
}