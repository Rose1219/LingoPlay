package com.lingolearn.entity;

import lombok.Data;
import org.hibernate.annotations.Type;

import javax.persistence.*;

/** 课时（一节课对应一种学习模块） */
@Entity
@Table(name = "lessons")
@Data
public class Lesson {

    public static final String TYPE_WORD = "WORD";
    public static final String TYPE_GRAMMAR = "GRAMMAR";
    public static final String TYPE_SPEAK = "SPEAK";
    public static final String TYPE_LISTEN = "LISTEN";
    public static final String TYPE_DIALOG = "DIALOG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(nullable = false, length = 100)
    private String title;

    /** 模块类型：WORD/GRAMMAR/SPEAK/LISTEN/DIALOG */
    @Column(nullable = false, length = 20)
    private String type;

    /** 结构化课程内容 JSON（格式随 type 变化）；TextType 兼容 MySQL 与 PostgreSQL */
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @Column(name = "content_json", nullable = false)
    private String contentJson;

    @Column(name = "sort_order")
    private Integer sortOrder;
}