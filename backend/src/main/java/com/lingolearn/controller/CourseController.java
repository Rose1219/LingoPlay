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

    // 游客只读：未登录时 userId 为 null，服务端只返回课程结构，不含任何个人进度
    @GetMapping
    public ApiResponse<List<CourseVO>> list(@RequestParam(required = false) String language) {
        return ApiResponse.ok(courseService.listCourses(language, AuthContext.getUserId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(courseService.courseDetail(id, AuthContext.getUserId()));
    }
}