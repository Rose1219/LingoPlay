package com.lingolearn.controller;

import com.lingolearn.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 发音接口：GET /api/tts?text=你好&lang=zh
 *
 * 直接返回 MP3 二进制。之所以用 GET 且匿名放行，是因为小程序 InnerAudioContext
 * 和 Web 的 Audio 元素都只能给一个 URL，没法带 Authorization 头。
 * 接口本身受 TtsService 的限长、限频、日配额保护。
 */
@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private static final Logger log = LoggerFactory.getLogger(TtsController.class);

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> speak(
            @RequestParam(name = "text", defaultValue = "") String text,
            @RequestParam(name = "lang", defaultValue = "en") String lang,
            @RequestParam(name = "rate", required = false) Double rate,
            HttpServletRequest request) {

        String ip = clientIp(request);
        try {
            TtsService.TtsResult result = ttsService.synthesize(text, lang, ip);
            if (result == null || result.getAudio() == null || result.getAudio().length == 0) {
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                        .body("{\"message\":\"暂无可用发音源\"}");
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, result.getContentType())
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .header("X-Tts-Provider", result.getProvider())
                    // 方言用普通话近似时置 1，客户端据此提示"方言发音开发中"
                    .header("X-Tts-Approximate", result.isApproximate() ? "1" : "0")
                    // 语速由客户端控制；有道端点不支持 rate，此处仅回显供调试
                    .header("X-Tts-Rate", String.valueOf(rate == null ? 1 : rate))
                    .body(result.getAudio());
        } catch (TtsService.TtsThrottledException e) {
            log.warn("tts throttled, ip={}", ip);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("{\"message\":\"" + e.getMessage() + "\"}");
        } catch (TtsService.TtsUnavailableException e) {
            // 501 让客户端知道"这条路线彻底没有"，从而熔断而不是每次都重试
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                    .body("{\"message\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            log.error("tts error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\":\"发音服务异常\"}");
        }
    }

    /** 取真实客户端 IP：优先平台注入的转发头，避免全站限流被同一个网关 IP 打满 */
    private String clientIp(HttpServletRequest request) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.trim().isEmpty() && !"unknown".equalsIgnoreCase(v.trim())) {
                return v.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
