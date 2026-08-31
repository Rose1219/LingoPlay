package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.dto.LessonDetailVO;
import com.lingolearn.dto.SubmitRequest;
import com.lingolearn.dto.SubmitResponse;
import com.lingolearn.security.AuthContext;
import com.lingolearn.service.CourseService;
import com.lingolearn.service.LearningService;
import org.springframework.web.bind.annotation.*;

/** 课时接口 */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final CourseService courseService;
    private final LearningService learningService;

    public LessonController(CourseService courseService, LearningService learningService) {
        this.courseService = courseService;
        this.learningService = learningService;
    }

    @GetMapping("/{id}")
    public ApiResponse<LessonDetailVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(courseService.lessonDetail(id, AuthContext.requireUserId()));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<Void> start(@PathVariable Long id) {
        learningService.startLesson(AuthContext.requireUserId(), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/submit")
    public ApiResponse<SubmitResponse> submit(@PathVariable Long id, @RequestBody SubmitRequest req) {
        return ApiResponse.ok(learningService.submitLesson(AuthContext.requireUserId(), id, req));
    }
}