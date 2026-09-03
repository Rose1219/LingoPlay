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

    /** 所属语种是否 VIP 专属（方言课程） */
    private Boolean vipOnly;

    /** 发音是否为近似（方言暂用普通话发音） */
    private Boolean ttsApproximate;
}