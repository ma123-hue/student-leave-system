package com.example.studentleave.webview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "StudentLeaveQA";

    private WebView webView;
    private TextView statusView;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView.setWebContentsDebuggingEnabled(true);
        setContentView(buildContentView());
        configureWebView();
        observeNetwork();

        if (savedInstanceState == null) {
            loadHome();
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    private View buildContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setPadding(12, 8, 12, 8);

        Button homeButton = new Button(this);
        homeButton.setText("首页");
        homeButton.setOnClickListener(v -> loadHome());

        Button reloadButton = new Button(this);
        reloadButton.setText("刷新");
        reloadButton.setOnClickListener(v -> webView.reload());

        statusView = new TextView(this);
        statusView.setText("正在检查网络…");
        statusView.setPadding(16, 12, 8, 8);

        toolbar.addView(homeButton);
        toolbar.addView(reloadButton);
        toolbar.addView(statusView, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        webView = new WebView(this);
        root.addView(toolbar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(webView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setUserAgentString(settings.getUserAgentString() + " StudentLeaveWebViewQA/1.0");

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage message) {
                Log.d(TAG, "WEB_CONSOLE " + message.messageLevel() + " " + message.sourceId() + ":" + message.lineNumber() + " " + message.message());
                return true;
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.i(TAG, "PAGE_START " + url);
                statusView.setText("加载中…");
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.i(TAG, "PAGE_FINISH " + url + " title=" + view.getTitle());
                updateNetworkStatus();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Log.e(TAG, "WEB_ERROR main=" + request.isForMainFrame() + " code=" + error.getErrorCode() + " url=" + request.getUrl() + " detail=" + error.getDescription());
                if (request.isForMainFrame()) {
                    statusView.setText("页面加载失败");
                    view.loadDataWithBaseURL(
                            BuildConfig.WEB_APP_URL,
                            offlinePageHtml(),
                            "text/html",
                            "UTF-8",
                            null);
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse response) {
                Log.e(TAG, "HTTP_ERROR main=" + request.isForMainFrame() + " status=" + response.getStatusCode() + " url=" + request.getUrl());
            }
        });
    }

    private void loadHome() {
        Log.i(TAG, "LOAD_HOME " + BuildConfig.WEB_APP_URL);
        webView.loadUrl(BuildConfig.WEB_APP_URL);
    }

    private String offlinePageHtml() {
        return "<!doctype html><html lang=\"zh-CN\"><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>body{margin:0;background:#f5f7fb;font-family:sans-serif;color:#1f2937}"
                + ".card{margin:22vh 24px 0;padding:32px 24px;background:#fff;border-radius:18px;"
                + "box-shadow:0 8px 30px rgba(15,23,42,.08);text-align:center}"
                + "h1{font-size:24px;margin:0 0 12px}p{color:#64748b;line-height:1.7}"
                + "a{display:block;margin-top:24px;padding:14px;border-radius:10px;background:#2563eb;"
                + "color:#fff;text-decoration:none;font-weight:700}</style></head><body>"
                + "<main class=\"card\"><h1>网络连接异常</h1>"
                + "<p>请检查 Wi-Fi 或移动网络，恢复后重新加载。</p>"
                + "<a href=\"" + BuildConfig.WEB_APP_URL + "\">重新加载</a></main></body></html>";
    }

    private void observeNetwork() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.i(TAG, "NETWORK_AVAILABLE " + network);
                runOnUiThread(() -> updateNetworkStatus());
            }

            @Override
            public void onLost(Network network) {
                Log.w(TAG, "NETWORK_LOST " + network);
                runOnUiThread(() -> statusView.setText("网络已断开"));
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
                Log.i(TAG, "NETWORK_CHANGED " + describeNetwork(capabilities));
                runOnUiThread(() -> updateNetworkStatus());
            }
        };
        connectivityManager.registerNetworkCallback(new NetworkRequest.Builder().build(), networkCallback);
        updateNetworkStatus();
    }

    private void updateNetworkStatus() {
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities capabilities = network == null ? null : connectivityManager.getNetworkCapabilities(network);
        String description = describeNetwork(capabilities);
        statusView.setText(description);
        Log.i(TAG, "NETWORK_STATUS " + description);
    }

    private String describeNetwork(NetworkCapabilities capabilities) {
        if (capabilities == null) return "无网络";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "Wi-Fi";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "移动网络";
        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "以太网";
        return "其他网络";
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        webView.destroy();
        super.onDestroy();
    }
}
