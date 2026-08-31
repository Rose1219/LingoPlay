package com.lingolearn.dto;

import lombok.Data;

/** 单词记忆结果项 */
@Data
public class WordAnswerItem {

    private String word;
    private String meaning;
    /** 是否回答正确 */
    private Boolean correct;
}