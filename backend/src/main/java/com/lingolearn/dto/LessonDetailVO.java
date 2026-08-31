package com.lingolearn.dto;

import lombok.Data;

/** 课时详情（含学习内容） */
@Data
public class LessonDetailVO {

    private Long id;
    private String title;
    private String type;
    private String status;
    private Integer bestScore;
    private Long courseId;
    private String courseTitle;
    private Long unitId;
    private String unitTitle;
    private String languageCode;
    /** 结构化内容（Object，由前端按 type 解析） */
    private Object content;
}