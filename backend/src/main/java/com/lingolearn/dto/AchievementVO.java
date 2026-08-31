package com.lingolearn.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 成就视图 */
@Data
public class AchievementVO {

    private Long id;
    private String code;
    private String name;
    private String description;
    private String icon;
    private String type;
    private String threshold;
    /** 当前用户是否已解锁 */
    private Boolean unlocked;
    private LocalDateTime unlockedAt;
}