package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.entity.Language;
import com.lingolearn.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 语种接口 */
@RestController
@RequestMapping("/api/languages")
public class LanguageController {

    private final CourseService courseService;

    public LanguageController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ApiResponse<List<Language>> list() {
        return ApiResponse.ok(courseService.listLanguages());
    }
}