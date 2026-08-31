package com.lingolearn.dto;

import lombok.Data;

/** 课时摘要 */
@Data
public class LessonBriefVO {

    private Long id;
    private String title;
    private String type;
    private Integer sortOrder;
    /** 学习状态：NOT_STARTED / IN_PROGRESS / COMPLETED */
    private String status;
    private Integer bestScore;
}