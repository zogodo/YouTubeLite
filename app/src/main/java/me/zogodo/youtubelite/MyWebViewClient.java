package me.zogodo.youtubelite;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.webkit.*;

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

    public void onPageStarted(WebView view, String url, Bitmap favicon)
    {
        Log.e("zzz " + mywebview.hashCode(), "onPageStarted " + url);
        /*
        String cookieStr = CookieTool.ReadCookie(MainActivity.me);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(url, cookieStr);
        */
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

        /*
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        String cookieStr = cookieManager.getCookie(url);
        CookieTool.SaveCookie(MainActivity.me, cookieStr);
        */

        super.onPageFinished(view, url);
    }

}
