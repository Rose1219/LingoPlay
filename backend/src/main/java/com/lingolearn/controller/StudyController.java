package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.dto.StudyStatsVO;
import com.lingolearn.security.AuthContext;
import com.lingolearn.service.StudyStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 学习统计接口 */
@RestController
@RequestMapping("/api/study")
public class StudyController {

    private final StudyStatsService studyStatsService;

    public StudyController(StudyStatsService studyStatsService) {
        this.studyStatsService = studyStatsService;
    }

    @GetMapping("/stats")
    public ApiResponse<StudyStatsVO> stats() {
        return ApiResponse.ok(studyStatsService.stats(AuthContext.requireUserId()));
    }
}