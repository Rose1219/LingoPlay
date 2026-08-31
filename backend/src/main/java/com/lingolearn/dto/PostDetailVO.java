package com.lingolearn.dto;

import lombok.Data;

import java.util.List;

/** 帖子详情（含评论） */
@Data
public class PostDetailVO {

    private PostVO post;
    private List<CommentVO> comments;
}