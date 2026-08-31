package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.dto.AchievementVO;
import com.lingolearn.security.AuthContext;
import com.lingolearn.service.AchievementService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 成就接口 */
@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping
    public ApiResponse<List<AchievementVO>> list() {
        return ApiResponse.ok(achievementService.listForUser(AuthContext.requireUserId()));
    }
}