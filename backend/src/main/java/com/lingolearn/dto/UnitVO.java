package com.lingolearn.dto;

import lombok.Data;

import java.util.List;

/** 单元视图 */
@Data
public class UnitVO {

    private Long id;
    private String title;
    private String description;
    private List<LessonBriefVO> lessons;
}