package com.mojumohan.webapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    // ==== Ningalude web app link ====
    private static final String APP_URL =
        "https://script.google.com/macros/s/AKfycbzBV00b_T9p06DFl19No86XZ9oUKd1jlCCiVXx_YYWx2vdNTe9CmhyZ_WhFrtwPQOnw/exec";


    // Google Apps Script-nte mukalile "This application was created by..." banner maraykkunnu
    private static final String HIDE_BANNER_JS =
        "(function(){function fix(){var f=document.getElementById('sandboxFrame')||document.querySelector('iframe');" +
        "if(!f)return;var keep=[];var n=f;while(n){keep.push(n);n=n.parentElement;}" +
        "var all=document.body?document.body.querySelectorAll('*'):[];" +
        "for(var i=0;i<all.length;i++){var e=all[i];if(keep.indexOf(e)<0&&e.tagName!='SCRIPT'&&e.tagName!='STYLE'){e.style.setProperty('display','none','important');}}" +
        "f.style.setProperty('position','fixed','important');f.style.setProperty('top','0','important');" +
        "f.style.setProperty('left','0','important');f.style.setProperty('width','100%','important');" +
        "f.style.setProperty('height','100%','important');f.style.setProperty('border','0','important');" +
        "f.style.setProperty('z-index','2147483647','important');}" +
        "fix();var c=0;var t=setInterval(function(){fix();if(++c>30)clearInterval(t);},300);})();";

    private void hideBanner(WebView v) {
        String u = v.getUrl();
        if (u != null && u.contains("script.google.com")) v.evaluateJavascript(HIDE_BANNER_JS, null);
    }

    private static final int FILE_CHOOSER = 1001;
    private WebView webView;
    private ProgressBar progress;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        webView = new WebView(this);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        root.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(progress, new FrameLayout.LayoutParams(-1, 12));
        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        // Google pages WebView-ine block cheyyathirikkan "; wv" marker maattunnu
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u = req.getUrl();
                String scheme = u.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) return false;
                // tel:, mailto:, whatsapp: thudangiyava purathu thurakkunnu
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) {}
                return true;
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                hideBanner(view);
            }
            @Override
            public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
                if (req.isForMainFrame()) {
                    view.loadData("<html><body style='font-family:sans-serif;text-align:center;padding-top:40%'>"
                        + "<h3>Internet connection illa</h3><p>Connection check cheythu veendum shramikkuka.</p>"
                        + "<button onclick=\"location.href='" + APP_URL + "'\" style='padding:12px 24px;font-size:16px'>Retry</button>"
                        + "</body></html>", "text/html", "UTF-8");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int p) {
                progress.setProgress(p);
                progress.setVisibility(p < 100 ? View.VISIBLE : View.GONE);
                if (p > 60) hideBanner(view);
            }
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) webView.restoreState(savedInstanceState);
        else webView.loadUrl(APP_URL);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == FILE_CHOOSER && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            fileCallback = null;
        }
        super.onActivityResult(req, res, data);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        webView.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
