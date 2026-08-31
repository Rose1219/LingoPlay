package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.common.PageResult;
import com.lingolearn.dto.*;
import com.lingolearn.security.AuthContext;
import com.lingolearn.service.CommunityService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/** 社区接口 */
@RestController
@RequestMapping("/api/posts")
public class CommunityController {

    private final CommunityService communityService;

    public CommunityController(CommunityService communityService) {
        this.communityService = communityService;
    }

    @GetMapping
    public ApiResponse<PageResult<PostVO>> posts(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam(required = false) String language) {
        return ApiResponse.ok(communityService.posts(
                Math.max(1, page), Math.min(50, Math.max(1, size)), language, AuthContext.requireUserId()));
    }

    @PostMapping
    public ApiResponse<PostVO> create(@Valid @RequestBody CreatePostRequest req) {
        return ApiResponse.ok(communityService.createPost(AuthContext.requireUserId(), req));
    }

    @GetMapping("/{id}")
    public ApiResponse<PostDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(communityService.postDetail(id, AuthContext.requireUserId()));
    }

    @PostMapping("/{id}/comments")
    public ApiResponse<CommentVO> comment(@PathVariable Long id, @Valid @RequestBody CommentRequest req) {
        return ApiResponse.ok(communityService.addComment(id, AuthContext.requireUserId(), req.getContent()));
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Map<String, Object>> like(@PathVariable Long id) {
        return ApiResponse.ok(communityService.toggleLike(id, AuthContext.requireUserId()));
    }
}