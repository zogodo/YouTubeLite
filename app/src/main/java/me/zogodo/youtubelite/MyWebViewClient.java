package me.zogodo.youtubelite;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayInputStream;

/**
 * Created by zogod on 17/2/19.
 */
public class MyWebViewClient extends WebViewClient
{
    MyWebView mywebview;

    public MyWebViewClient(MyWebView mywebview)
    {
        this.mywebview = mywebview;
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload)
    {
        //正常情况下youtube点击视频播放时,仅仅是修改了url,并不能触发shouldOverrideUrlLoading()
        //此时的网页变化是由js修改的,安卓无法介入,view.stopLoading()没用.
        //解决办法时把所有<a>的taget都改成_blank,使用新窗口打开.
        Log.e("zzz " + mywebview.hashCode(), "doUpdateVisitedHistory " + url);
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req)
    {
        Log.e("zzz " + mywebview.hashCode(), "shouldOverrideUrlLoading " + req.getUrl().toString());
        return false;
    }

    private final String[] adDomains = {
            "googleads.g.doubleclick.net",
            "pubads.g.doubleclick.net",
            "adservice.google.com",
            "youtube.com/pagead/"
    };

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        for (String domain : adDomains) {
            if (url.contains(domain)) {
                // 返回空响应
                return new WebResourceResponse("text/plain", "utf-8",
                        new ByteArrayInputStream("".getBytes()));
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon)
    {
        Log.e("zzz " + mywebview.hashCode(), "onPageStarted " + url);

        String domain = url.replaceAll("https?://(www.)?([^/]+).*", "$2");
        String cookieStr = CookieTool.ReadCookie(MainActivity.me, domain);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(url, cookieStr);

        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onLoadResource(WebView view, String url)
    {
        super.onLoadResource(view, url);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onPageFinished(WebView view, String url)
    {
        Log.e("zzz " + mywebview.hashCode(), "onPageFinished " + url);
        this.mywebview.loadUrl("javascript:" + MyWebView.myJs);
        this.mywebview.injectCSS();

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        String cookieStr = cookieManager.getCookie(url);
        String domain = url.replaceAll("https?://(www.)?([^/]+).*", "$2");
        CookieTool.SaveCookie(MainActivity.me, cookieStr, domain);

        /*
        // 1. 构建你的 HTML 字符串，注意：在 URL 后面追加了 &origin=https://www.youtube.com
        String htmlData = "<!DOCTYPE html><html><head><style>body{margin:0;padding:0;background:#000;}iframe{width:100%;height:100%;position:absolute;top:0;left:0;border:none;}</style></head><body>" +
                "<iframe src='https://www.youtube.com/embed/Srfin-7c3Co?si=MKpy4zU5-uGxoO9g&origin=https://www.youtube.com' " +
                "allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share' allowfullscreen></iframe>" +
                "</body></html>";

        // 2. 使用 loadDataWithBaseURL 进行加载
        // 关键在于第一个参数 baseUrl，我们填入 https://www.youtube.com 
        // 这样 WebView 内部发出的所有网络请求都会默认带上 YouTube 官方的域，从而完美绕过错误 153
        webView.loadDataWithBaseURL("https://www.youtube.com", htmlData, "text/html", "UTF-8", null);
        */

        super.onPageFinished(view, url);
    }

}
