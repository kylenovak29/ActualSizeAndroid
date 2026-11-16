package org.actualsize.viewer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.RenderProcessGoneDetail;
import android.widget.FrameLayout;
import androidx.webkit.WebViewAssetLoader;

/** A thin native shell for the unmodified Actual Size HTML v4 application. */
public final class MainActivity extends Activity {
    private static final String START_URL =
            "https://appassets.androidplatform.net/assets/www/index.html";
    private static final int PHOTO_REQUEST = 42;

    private WebView webView;
    private ValueCallback<Uri[]> pendingPhotoResult;
    private FrameLayout root;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Keep the webpage in the usable area on modern edge-to-edge Android devices.
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(16, 24, 32));
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });
        }

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(16, 24, 32));
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true); // Persist only calibration/preferences.
        settings.setAllowFileAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        // Content access is needed for the image the user explicitly selects.
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportZoom(false); // The HTML implements its own touch zoom.
        settings.setBuiltInZoomControls(false);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Only the bundled app can be navigated to. No online pages are needed.
                return !START_URL.equals(request.getUrl().toString());
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // An Android WebView renderer crash must not leave a stale WebView attached.
                root.removeView(view);
                view.destroy();
                webView = null;
                finish();
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view,
                    ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingPhotoResult != null) pendingPhotoResult.onReceiveValue(null);
                pendingPhotoResult = callback;

                // System document/photo picker: no broad storage/media permissions.
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    startActivityForResult(intent, PHOTO_REQUEST);
                } catch (ActivityNotFoundException ex) {
                    pendingPhotoResult.onReceiveValue(null);
                    pendingPhotoResult = null;
                }
                return true;
            }
        });
        webView.loadUrl(START_URL);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PHOTO_REQUEST || pendingPhotoResult == null) return;
        Uri selected = (resultCode == RESULT_OK && data != null) ? data.getData() : null;
        if (selected != null && !"content".equalsIgnoreCase(selected.getScheme())) {
            selected = null;
        }
        if (selected != null) {
            String type = getContentResolver().getType(selected);
            if (type != null && !type.startsWith("image/")) selected = null;
        }
        pendingPhotoResult.onReceiveValue(selected == null ? null : new Uri[]{selected});
        pendingPhotoResult = null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView == null) {
            super.onBackPressed();
            return;
        }
        // Android Back should close the HTML's overlays before exiting the app.
        webView.evaluateJavascript(
                "(function(){var a=document.getElementById('actualOverlay'),"
              + "c=document.getElementById('cardOverlay');"
              + "if(a&&!a.hidden){document.getElementById('backActual').click();return true;}"
              + "if(c&&!c.hidden){document.getElementById('closeCard').click();return true;}"
              + "return false;})()",
                value -> { if (!"true".equals(value)) MainActivity.super.onBackPressed(); });
    }

    @Override
    protected void onDestroy() {
        if (pendingPhotoResult != null) {
            pendingPhotoResult.onReceiveValue(null);
            pendingPhotoResult = null;
        }
        if (webView != null) {
            root.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
