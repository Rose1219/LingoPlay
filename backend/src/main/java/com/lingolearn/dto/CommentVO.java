package com.lingolearn.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 评论视图 */
@Data
public class CommentVO {

    private Long id;
    private Long authorId;
    private String authorNickname;
    private String authorAvatar;
    private String content;
    private LocalDateTime createdAt;
}