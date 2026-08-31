package com.lingolearn.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/** 课程（按语种 + 等级划分） */
@Entity
@Table(name = "courses")
@Data
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "language_id", nullable = false)
    private Language language;

    @Column(nullable = false, length = 100)
    private String title;

    /** CEFR 等级：A1/A2/B1/B2/C1 */
    @Column(nullable = false, length = 10)
    private String level;

    /** 等级中文名：入门/初级/中级 … */
    @Column(name = "level_name", length = 20)
    private String levelName;

    @Column(length = 255)
    private String description;

    /** 封面 emoji */
    @Column(length = 10)
    private String cover;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /** 课程内单元数（冗余，用于列表展示） */
    @Column(name = "unit_count")
    private Integer unitCount;

    /** 课程内课时数（冗余，用于列表展示） */
    @Column(name = "lesson_count")
    private Integer lessonCount;

    @OneToMany(mappedBy = "course")
    private List<Unit> units = new ArrayList<>();
}