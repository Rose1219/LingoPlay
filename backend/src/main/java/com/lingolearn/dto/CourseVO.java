package com.lingolearn.dto;

import lombok.Data;

/** 课程列表项 */
@Data
public class CourseVO {

    private Long id;
    private Long languageId;
    private String languageCode;
    private String languageName;
    private String title;
    private String level;
    private String levelName;
    private String description;
    private String cover;
    private Integer unitCount;
    private Integer lessonCount;
    private Integer completedLessons;
    private Integer inProgressLessons;
    /** 完成进度百分比 0-100 */
    private Integer progressPercent;
}