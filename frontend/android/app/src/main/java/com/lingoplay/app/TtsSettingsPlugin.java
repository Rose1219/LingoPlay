package com.lingoplay.app;

import android.content.Intent;
import android.provider.Settings;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

/**
 * 跳转系统语音设置的原生插件。
 *
 * 国产 ROM 默认 TTS 引擎常缺英语/日语/韩语语音数据，导致 App 内发音无声；
 * 提示文字用户难找入口，此插件一键跳到系统的「文字转语音（TTS）输出」设置页，
 * 部分旧 ROM 无独立 TTS 页时退回到「语言和输入法」页。
 */
@CapacitorPlugin(name = "TtsSettings")
public class TtsSettingsPlugin extends Plugin {

    @PluginMethod
    public void open(PluginCall call) {
        try {
            Intent intent = new Intent("com.android.settings.TTS_SETTINGS");
            // 该 ROM 无独立 TTS 设置页时，退回「语言和输入法」设置
            if (intent.resolveActivity(getContext().getPackageManager()) == null) {
                intent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("open tts settings failed", e);
        }
    }
}
