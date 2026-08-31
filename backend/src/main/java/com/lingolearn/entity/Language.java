package com.lingolearn.entity;

import lombok.Data;

import javax.persistence.*;

/** 语种 */
@Entity
@Table(name = "languages")
@Data
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 代码：en/ja/ko */
    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "name_cn", nullable = false, length = 50)
    private String nameCn;

    /** 图标 emoji */
    @Column(length = 10)
    private String icon;

    @Column(length = 255)
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder;
}