package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.dto.SubmitRequest;
import com.lingolearn.security.AuthContext;
import com.lingolearn.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 游戏化接口 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/daily-word")
    public ApiResponse<Map<String, Object>> dailyWord(@RequestParam(required = false) String lang,
                                                      @RequestParam(defaultValue = "false") boolean random) {
        // 游客可看每日单词（内容只读，不入单词本）；登录后自动关联进度
        return ApiResponse.ok(gameService.dailyWord(AuthContext.getUserId(), lang, random));
    }

    /** 单词闯关词库（按语种汇总全部单词） */
    @GetMapping("/word-quiz")
    public ApiResponse<List<Map<String, String>>> quizWords(@RequestParam(required = false) String lang) {
        return ApiResponse.ok(gameService.quizWords(AuthContext.getUserId(), lang));
    }

    /** 单词闯关成绩提交 */
    @PostMapping("/word-quiz/submit")
    public ApiResponse<Map<String, Object>> submitQuiz(@RequestBody SubmitRequest req) {
        return ApiResponse.ok(gameService.submitQuiz(AuthContext.requireUserId(), req));
    }
}