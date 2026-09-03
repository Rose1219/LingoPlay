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

    /**
     * 是否仅限 VIP 使用。
     * 汉语方言（粤语/四川话/北京话/上海话）因需真人录音或商业方言音色，
     * 成本显著高于标准语种，故设为 VIP 专属。
     */
    @Column(name = "vip_only")
    private Boolean vipOnly = false;

    /**
     * 是否没有真实发音源（方言目前用普通话近似）。
     * 前端据此显示「方言发音开发中」，避免用户误以为听到的就是方言。
     */
    @Column(name = "tts_approximate")
    private Boolean ttsApproximate = false;

    /** 该方言/语种对应的普通话说法，用于词库释义对照 */
    @Column(name = "fallback_to", length = 10)
    private String fallbackTo;
}