package me.zogodo.youtubelite;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.adblockplus.libadblockplus.android.webview.AdblockWebView;

import java.util.Stack;

/**
 * Created by zogod on 17/2/19.
 */
public class MyWebView extends AdblockWebView
{
    //region 共有变量
    public static Stack<MyWebView> webview_stack = null;
    public static String myJs = null;
    public static String myCss = "";
    //endregion

    //region 构造器
    public MyWebView()
    {
        super(MainActivity.me);
        if (MyWebView.webview_stack == null)
        {
            MyWebView.webview_stack = new Stack<>();
            MyWebView.myJs = CookieTool.RawFileToString(MainActivity.me, R.raw.myjs);
        }
        MyWebView.webview_stack.push(this);
        MainActivity.me.setContentView(this);
        //MainActivity.webView = this;
        this.WebViewInit();
    }
    public MyWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    //endregion

    //region goBack
    public boolean canGoBack()
    {
        Log.e("zzz " + this.hashCode(), "canGoBack " + MyWebView.webview_stack.size() + super.canGoBack());
        return MyWebView.webview_stack.size() > 1 || super.canGoBack();
    }
    public void goBack()
    {
        Log.e("zzz " + this.hashCode(), "goBack " + MyWebView.webview_stack.size() + super.canGoBack());
        if (super.canGoBack()) {
            super.goBack();
            return;
        }
        MyWebView.webview_stack.pop();
        MyWebView old_mywebview = MyWebView.webview_stack.peek();
        MainActivity.me.setContentView(old_mywebview);
    }
    //endregion

    @Override
    public void loadUrl(String url)
    {
        super.loadUrl(url);
    }

    //https://stackoverflow.com/questions/52028940
    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility != View.GONE) super.onWindowVisibilityChanged(View.VISIBLE);
    }

    public void injectCSS()
    {
        this.loadUrl("javascript:(function() {" +
                "var parent = document.getElementsByTagName('head').item(0);" +
                "var style = document.createElement('style');" +
                "style.type = 'text/css';" +
                "style.innerHTML = '" + MyWebView.myCss + "';" +
                "parent.appendChild(style)" +
                "})()");
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void WebViewInit()
    {
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setSupportMultipleWindows(true);
        this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAppCacheEnabled(true);
        this.setWebViewClient(new MyWebViewClient(this));
        //这两个是要在 Chrome inspect 调试时用的
        this.setWebChromeClient(new MyWebChromeClient());
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            this.setWebContentsDebuggingEnabled(true);
        }
    }
}


