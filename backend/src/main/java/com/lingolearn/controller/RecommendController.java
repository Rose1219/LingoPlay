package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.dto.RecommendVO;
import com.lingolearn.security.AuthContext;
import com.lingolearn.service.RecommendService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 个性化推荐接口 */
@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping
    public ApiResponse<RecommendVO> recommend() {
        return ApiResponse.ok(recommendService.recommend(AuthContext.requireUserId()));
    }
}