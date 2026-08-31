package com.lingolearn.dto;

import lombok.Data;

/** 推荐：待复习单词 */
@Data
public class ReviewWordVO {

    private String word;
    private String meaning;
    private String languageCode;
    /** 掌握度 0-4 */
    private Integer mastery;
}