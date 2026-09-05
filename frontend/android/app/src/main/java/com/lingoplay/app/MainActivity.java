package com.lingoplay.app;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.graphics.Color;
import android.view.Gravity;

import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private View errorView;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enhance WebView configuration for better performance and functionality
        Bridge bridge = getBridge();
        if (bridge != null) {
            WebView webView = bridge.getWebView();
            if (webView != null) {
                WebSettings webSettings = webView.getSettings();

                // Enable JavaScript
                webSettings.setJavaScriptEnabled(true);

                // Enable DOM storage
                webSettings.setDomStorageEnabled(true);

                // Allow file access from file URLs (if needed for local resources)
                webSettings.setAllowFileAccessFromFileURLs(false);
                webSettings.setAllowUniversalAccessFromFileURLs(false);

                // Enable cache
                webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);

                // Enable database storage API
                webSettings.setDatabaseEnabled(true);

                // Enable geolocation
                webSettings.setGeolocationEnabled(true);

                // Support multiple windows
                webSettings.setSupportMultipleWindows(true);

                // Support zoom
                webSettings.setSupportZoom(true);
                webSettings.setBuiltInZoomControls(true);
                webSettings.setDisplayZoomControls(false);

                // Enable smooth transition
                webSettings.setLoadWithOverviewMode(true);
                webSettings.setUseWideViewPort(true);

                // Enable hardware acceleration if available
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                } else {
                    webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                }

                // Set custom WebViewClient for error handling and offline support
                webView.setWebViewClient(new CustomWebViewClient());

                // Add progress bar
                addProgressBar();
            }
        }

        // 一键跳转系统 TTS 语音设置（发音无声时引导用户安装语音数据）
        registerPlugin(TtsSettingsPlugin.class);
    }

    /**
     * Custom WebViewClient to handle errors, offline detection, and loading states
     */
    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            showLoading(true);
            hideErrorView();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            showLoading(false);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error);
            showLoading(false);
            showErrorView("网络加载失败，请检查您的网络连接并重试。", error.getDescription().toString());
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
            showLoading(false);
            showErrorView("服务器返回错误 (HTTP " + errorResponse.getStatusCode() + ")",
                    "请稍后重试或联系支持。");
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            // Let the WebView handle the URL loading
            return false;
        }
    }

    /**
     * Add progress bar to the layout
     */
    private void addProgressBar() {
        // Create a frame layout to hold the webview and progress bar
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(android.R.id.content);

        // Get the existing content view (should be the webview from BridgeActivity)
        View contentView = getWindow().getDecorView().findViewById(android.R.id.content);
        ViewGroup root = (ViewGroup) contentView.getParent();
        int index = root.indexOfChild(contentView);

        // Remove the original content view
        root.removeView(contentView);

        // Add the frame layout
        root.addView(frameLayout, index, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // Add the original content view back to the frame
        frameLayout.addView(contentView);

        // Create and add progress bar
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.GONE);
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        progressParams.gravity = Gravity.CENTER;
        progressBar.setLayoutParams(progressParams);
        frameLayout.addView(progressBar);

        // Create and add error view (initially hidden)
        errorView = createErrorView();
        errorView.setVisibility(View.GONE);
        frameLayout.addView(errorView);
    }

    /**
     * Create error view to show when loading fails
     */
    private View createErrorView() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);
        layout.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("加载失败");
        title.setTextSize(20);
        title.setTextColor(Color.DKGRAY);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);

        TextView message = new TextView(this);
        message.setText("请检查网络连接并重试");
        message.setTextSize(16);
        message.setTextColor(Color.GRAY);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 0, 0, 20);
        layout.addView(message);

        TextView details = new TextView(this);
        details.setText("");
        details.setTextSize(14);
        details.setTextColor(Color.LTGRAY);
        details.setGravity(Gravity.CENTER);
        details.setVisibility(View.GONE);
        layout.addView(details);

        // Store reference to details view for updating
        ((LinearLayout) layout).getChildAt(2).setId(View.generateViewId());

        return layout;
    }

    /**
     * Show or hide loading progress bar
     */
    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Show error view with message and details
     */
    private void showErrorView(String title, String details) {
        if (errorView != null) {
            // Update title
            TextView titleView = errorView.findViewById(android.R.id.text1);
            if (titleView == null) {
                // Find by position since we don't have IDs set
                LinearLayout layout = (LinearLayout) errorView;
                if (layout.getChildCount() > 0) {
                    titleView = (TextView) layout.getChildAt(0);
                    if (titleView != null) {
                        titleView.setText(title);
                    }
                }
            } else {
                titleView.setText(title);
            }

            // Update details
            TextView detailsView = errorView.findViewById(View.generateViewId() - 2); // Approximate
            if (detailsView == null) {
                LinearLayout layout = (LinearLayout) errorView;
                if (layout.getChildCount() > 2) {
                    detailsView = (TextView) layout.getChildAt(2);
                    if (detailsView != null) {
                        detailsView.setText(details);
                        detailsView.setVisibility(!details.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            } else {
                detailsView.setText(details);
                detailsView.setVisibility(!details.isEmpty() ? View.VISIBLE : View.GONE);
            }

            errorView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide error view
     */
    private void hideErrorView() {
        if (errorView != null) {
            errorView.setVisibility(View.GONE);
        }
    }
}
