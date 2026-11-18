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

    // 广告相关请求的 URL 匹配子串(第一层拦截,返回空响应中断广告脚本/图片/上报)
    // NOTE: 不要加 youtubei/v1/player 等会影响页面正常功能的接口
    private static final String[] adUrlPatterns = {
            "googleads.g.doubleclick.net",
            "pubads.g.doubleclick.net",
            "adservice.google.com",
            "doubleclick.net",                    // googleads.g / pubads.g / securepubads.g / partnerad 等全部 Google 广告服务器
            "googlesyndication.com",              // pagead2(广告JS) / tpc(广告图片) / adservice
            "googleadservices.com",
            "googletagservices.com",
            "youtube.com/ad_companion",
            "/get_midroll_",                       // 老的片头/片中广告接口
            "youtube.com/pagead/",
            "youtube.com/api/stats/ads",
            "youtube.com/youtubei/v1/player/ad_break",
            "ads.youtube.com",
            "youtube.com/api/stats/qoe"
    };

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        String url = request.getUrl().toString();
        for (String pattern : adUrlPatterns) {
            if (url.contains(pattern)) {
                // 拦截广告请求,返回空响应
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

        //视频页/短视频页是新WebView冷打开,没有用户手势,YouTube会静音自动播放;
        //尽早开始轮询"点按取消静音"提示,出现后注入真实触摸解除(等效用户手点)
        if (url != null && (url.contains("/watch") || url.contains("/shorts")))
        {
            this.mywebview.StartUnmuteTapPolling();
        }

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

        super.onPageFinished(view, url);
    }

}
