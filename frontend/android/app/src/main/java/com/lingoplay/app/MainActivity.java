package com.lingoplay.app;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 一键跳转系统 TTS 语音设置（发音无声时引导用户安装语音数据）
        registerPlugin(TtsSettingsPlugin.class);
    }
}
