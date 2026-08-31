package com.lingolearn.dto;

import lombok.Data;

/** 推荐：继续学习的课时 */
@Data
public class ContinueLessonVO {

    private Long lessonId;
    private String title;
    private String type;
    private String courseTitle;
    private String unitTitle;
    private String languageCode;
    private String languageIcon;
}