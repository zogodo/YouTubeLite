package me.zogodo.youtubelite;

import android.content.Context;
import android.os.Build;

import android.util.AttributeSet;
import android.view.*;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.Stack;

/**
 * Created by zogod on 17/2/19.
 */
public class MyWebView extends WebView
{
    //region 共有变量
    public static Stack<MyWebView> webview_stack = null;
    public final static String myJs = "window.addEventListener('scroll', function () {\n" +
            "    var links = document.querySelectorAll('a:not([target=_blank])');\n" +
            "    for (var i = 0; i < links.length; i++) {\n" +
            "        links[i].target = '_blank';\n" +
            "    }\n" +
            "});";
    public static String myCss = "";
    //endregion

    //region 构造器
    public MyWebView(String url)
    {
        super(MainActivity.me);
        if (MyWebView.webview_stack == null)
        {
            MyWebView.webview_stack = new Stack<>();
        }
        MyWebView.webview_stack.push(this);
        this.WebViewInit(url);
    }
    public MyWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    //endregion

    //region goBack
    public boolean canGoBack()
    {
        return MyWebView.webview_stack.size() > 1;
    }
    public void goBack()
    {
        MyWebView.webview_stack.pop();
        MyWebView old_mywebview = MyWebView.webview_stack.peek();

        old_mywebview.StartView();
    }
    //endregion

    public void StartView()
    {
        MainActivity.me.setContentView(this);
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

    public void WebViewInit(String url)
    {
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setSupportMultipleWindows(true);
        this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.setWebViewClient(new MyWebViewClient(this));
        //这两个是要在 Chrome inspect 调试时用的
        this.setWebChromeClient(new MyWebChromeClient());
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            this.setWebContentsDebuggingEnabled(true);
        }
        this.loadUrl(url);
    }
}


