package com.lingolearn.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 帖子视图 */
@Data
public class PostVO {

    private Long id;
    private Long authorId;
    private String authorNickname;
    private String authorAvatar;
    private Long languageId;
    private String languageName;
    private String languageIcon;
    private String title;
    private String content;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createdAt;
    /** 当前用户是否已点赞 */
    private Boolean liked;
}