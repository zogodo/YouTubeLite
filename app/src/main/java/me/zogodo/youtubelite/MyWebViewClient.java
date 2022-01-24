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

    public MyWebViewClient(MyWebView mywebview) {
        this.mywebview = mywebview;
    }

    @Override
    public void doUpdateVisitedHistory(WebView view, String url, boolean isReload)
    {
        Log.e("zzz doUpdateVisitedHistory", url);
        super.doUpdateVisitedHistory(view, url, isReload);
    }

    @Override
    @TargetApi(24)
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req)
    {
        Log.e("zzz shouldOverrideUrlLoading", req.getUrl().toString());
        return false;
    }

    public void onPageStarted(WebView view, String url, Bitmap favicon)
    {
        Log.e("zzz onPageStarted", url);
        super.onPageStarted(view, url, favicon);
    }

    @Override
    public void onLoadResource(WebView view, String url)
    {
        //Log.e("zzz onLoadResource", url);
        super.onLoadResource(view, url);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void onPageFinished(WebView view, String url)
    {
        // Log.e("ended url", url);
        this.mywebview.loadUrl("javascript:" + MyWebView.myJs);
        this.mywebview.injectCSS();
        super.onPageFinished(view, url);
    }

}
