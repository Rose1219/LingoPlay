package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import com.lingolearn.dto.TranslateRequest;
import com.lingolearn.dto.TranslateResponse;
import com.lingolearn.service.TranslationLanguages;
import com.lingolearn.service.TranslationService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 翻译接口：多语种互译 + 语种自动检测 */
@RestController
@RequestMapping("/api/translate")
public class TranslationController {

    private final TranslationService translationService;

    public TranslationController(TranslationService translationService) {
        this.translationService = translationService;
    }

    /** 翻译。source 传 auto 或留空则自动检测源语种 */
    @PostMapping
    public ApiResponse<TranslateResponse> translate(@Valid @RequestBody TranslateRequest req) {
        return ApiResponse.ok(translationService.translate(req.getText(), req.getSource(), req.getTarget()));
    }

    /** 语种检测，返回归一化后的语种 code */
    @PostMapping("/detect")
    public ApiResponse<Map<String, Object>> detect(@RequestBody Map<String, String> body) {
        String text = body == null ? null : body.get("text");
        String lang = translationService.detect(text);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("language", lang);
        data.put("name", TranslationLanguages.displayName(lang));
        return ApiResponse.ok(data);
    }

    /** 可选语种列表，供前端下拉框使用 */
    @GetMapping("/languages")
    public ApiResponse<List<Map<String, Object>>> languages() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (TranslationLanguages.Lang l : translationService.languages()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", l.code);
            m.put("nativeName", l.nativeName);
            m.put("englishName", l.englishName);
            m.put("chineseName", l.chineseName);
            m.put("flag", l.flag);
            list.add(m);
        }
        return ApiResponse.ok(list);
    }
}
