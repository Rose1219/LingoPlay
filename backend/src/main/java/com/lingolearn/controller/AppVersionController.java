package com.lingolearn.controller;

import com.lingolearn.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** App 客户端版本接口：客户端据此检查更新（免登录） */
@RestController
@RequestMapping("/api/app")
public class AppVersionController {

    @Value("${app.latest.version-name:}")
    private String versionName;

    @Value("${app.latest.version-code:0}")
    private int versionCode;

    @Value("${app.latest.update-notes:}")
    private String updateNotes;

    @Value("${app.latest.force-update:false}")
    private boolean forceUpdate;

    @Value("${app.latest.android-file:}")
    private String androidFile;

    @Value("${app.latest.ios-file:}")
    private String iosFile;

    /** 最新版本信息：版本号、更新说明、下载文件名（完整 URL 由客户端按站点地址拼接） */
    @GetMapping("/latest")
    public ApiResponse<Map<String, Object>> latest() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("versionName", versionName);
        data.put("versionCode", versionCode);
        data.put("updateNotes", updateNotes);
        data.put("forceUpdate", forceUpdate);
        data.put("androidFile", androidFile);
        data.put("iosFile", iosFile);
        return ApiResponse.ok(data);
    }
}
