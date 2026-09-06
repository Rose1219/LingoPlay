package com.lingoplay.app;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@CapacitorPlugin(name = "AndroidTts")
public class AndroidTtsPlugin extends Plugin implements TextToSpeech.OnInitListener {

    private static final String TAG = "AndroidTtsPlugin";
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private String lastUtteranceId = "";

    @Override
    protected void handleOnDestroy() {
        if (tts != null) {
            tts.shutdown();
        }
        super.handleOnDestroy();
    }

    @PluginMethod
    public void speak(PluginCall call) {
        String text = call.getString("text");
        String lang = call.getString("lang", "en");
        Double rate = call.getDouble("rate", 1.0);

        if (text == null || text.trim().isEmpty()) {
            call.reject("Text to speak is required");
            return;
        }

        if (!ttsReady) {
            call.reject("TTS engine not initialized");
            return;
        }

        // Set language
        Locale locale = parseLocale(lang);
        int result = tts.setLanguage(locale);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language not supported for TTS: " + lang);
            // Try fallback to default
            locale = Locale.getDefault();
            tts.setLanguage(locale);
        }

        // Set speech rate
        tts.setSpeechRate(rate.floatValue());

        // Speak
        String utteranceId = String.valueOf(System.currentTimeMillis());
        lastUtteranceId = utteranceId;
        int speakResult = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        if (speakResult == TextToSpeech.SUCCESS) {
            call.resolve();
        } else {
            call.reject("Failed to start TTS");
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        if (tts != null) {
            tts.stop();
        }
        call.resolve();
    }

    @PluginMethod
    public void getSupportedLanguages(PluginCall call) {
        if (tts == null) {
            call.reject("TTS not initialized");
            return;
        }
        @SuppressWarnings("deprecation")
        Set<Locale> localeSet = tts.getAvailableLanguages();
        List<String> languages = new ArrayList<>();
        for (Locale locale : localeSet) {
            String lang = locale.getLanguage();
            String country = locale.getCountry();
            if (!country.isEmpty()) {
                lang += "-" + country;
            }
            // Add variant if needed (API < 21 maybe)
            String variant = locale.getVariant();
            if (!variant.isEmpty()) {
                lang += "_" + variant;
            }
            languages.add(lang);
        }
        JSObject ret = new JSObject();
        ret.put("languages", languages);
        call.resolve(ret);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true;
            Log.i(TAG, "TTS engine initialized successfully");
            // Optional: set an utterance progress listener to know when speech starts/ends/completed
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    // Speech started
                }

                @Override
                public void onDone(String utteranceId) {
                    // Speech completed
                    if (utteranceId.equals(lastUtteranceId)) {
                        // Notify JS? Not needed for now
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    // Speech error
                }
            });
        } else {
            ttsReady = false;
            Log.e(TAG, "TTS engine initialization failed, status: " + status);
        }
    }

    /**
     * Parse language string like "en", "zh-CN", "zh-yue" into Locale.
     * For unsupported locales, fallback to default.
     */
    private Locale parseLocale(String langTag) {
        if (langTag == null || langTag.isEmpty()) {
            return Locale.getDefault();
        }
        // Replace underscore with hyphen for consistency
        langTag = langTag.replace('_', '-');
        String[] parts = langTag.split("-");
        String language = parts[0].toLowerCase();
        String country = parts.length > 1 ? parts[1].toUpperCase() : "";
        String variant = parts.length > 2 ? parts[2] : "";

        if (!country.isEmpty() && !variant.isEmpty()) {
            return new Locale(language, country, variant);
        } else if (!country.isEmpty()) {
            return new Locale(language, country);
        } else {
            return new Locale(language);
        }
    }

    // Initialize TTS when plugin is loaded
    @Override
    public void load() {
        super.load();
        Context ctx = getContext();
        tts = new TextToSpeech(ctx, this);
    }
}