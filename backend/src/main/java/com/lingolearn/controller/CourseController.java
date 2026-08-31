package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.dto.CourseVO;
import com.lingolearn.security.AuthContext;
import com.lingolearn.service.CourseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 课程接口 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ApiResponse<List<CourseVO>> list(@RequestParam(required = false) String language) {
        return ApiResponse.ok(courseService.listCourses(language, AuthContext.requireUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(courseService.courseDetail(id, AuthContext.requireUserId()));
    }
}