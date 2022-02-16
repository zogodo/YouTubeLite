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
        Log.e("zzz doUpdateVisitedHistory", url);
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.N)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req)
    {
        Log.e("zzz shouldOverrideUrlLoading", req.getUrl().toString());
        return false;
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon)
    {
        Log.e("zzz onPageStarted", url);
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
        // Log.e("ended url", url);
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
