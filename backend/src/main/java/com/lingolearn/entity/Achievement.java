package com.lingolearn.entity;

import lombok.Data;

import javax.persistence.*;

/** 成就定义 */
@Entity
@Table(name = "achievements")
@Data
public class Achievement {

    /** 类型：统计完成课时数 */
    public static final String TYPE_LESSONS = "LESSONS";
    /** 类型：统计掌握单词数 */
    public static final String TYPE_WORDS = "WORDS";
    /** 类型：统计连续打卡天数 */
    public static final String TYPE_STREAK = "STREAK";
    /** 类型：统计发帖数 */
    public static final String TYPE_POSTS = "POSTS";
    /** 类型：统计获赞数 */
    public static final String TYPE_LIKES = "LIKES";
    /** 类型：完成指定等级课程 */
    public static final String TYPE_COURSE_LEVEL = "COURSE_LEVEL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /** 图标 emoji */
    @Column(length = 10)
    private String icon;

    @Column(nullable = false, length = 30)
    private String type;

    /** 达成阈值（COURSE_LEVEL 类型存等级代码如 A1） */
    @Column(length = 50)
    private String threshold;
}