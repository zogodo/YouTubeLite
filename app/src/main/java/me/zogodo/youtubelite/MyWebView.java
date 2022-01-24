package me.zogodo.youtubelite;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.util.Stack;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * Created by zogod on 17/2/19.
 */
public class MyWebView extends WebView
{
    //region 共有变量
    public static Stack<MyWebView> webview_stack = null;
    public static String myJs = "window.addEventListener('scroll', function () {\n" +
            "    var links = document.querySelectorAll('a:not([target=_blank])');\n" +
            "    for (var i = 0; i < links.length; i++) {\n" +
            "        links[i].target = '_blank';\n" +
            "    }\n" +
            "});";
    public static String myCss = "";
    public SwipeRefreshLayout swipe_refresh_layout = null;
    public int screen_width;
    public int screen_height;
    //endregion

    //region 构造器
    public MyWebView()
    {
        super(MainActivity.me);
        if (MyWebView.webview_stack == null)
        {
            MyWebView.webview_stack = new Stack<>();
        }
        MyWebView.webview_stack.push(this);
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
        Object ob = this.getParent();
        if (ob != null)
        {
            this.swipe_refresh_layout = (SwipeRefreshLayout)ob;
            this.BindFresh(this.swipe_refresh_layout);
            ConstraintLayout rel_layout = (ConstraintLayout)this.swipe_refresh_layout.getParent();
            MainActivity.me.setContentView(rel_layout);
        }
        else
        {
            MainActivity.me.setContentView(this);
        }
    }

    //https://stackoverflow.com/questions/52028940/how-can-i-make-webview-keep-a-video-or-audio-playing-in-the-background
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

    public void WebViewInit(String url, String js, String css)
    {
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setSupportMultipleWindows(true);
        this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.setWebViewClient(new MyWebViewClient(this));

        //region 这两个是要在 Chrome inspect 调试时用的
        this.setWebChromeClient(new WebChromeClient() // alert() 要用
        {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm)
            {
                Log.d("MyApplication", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg)
            {
                LayoutInflater inflater = (LayoutInflater) MainActivity.me.getSystemService(LAYOUT_INFLATER_SERVICE);
                ConstraintLayout rel_layout = (ConstraintLayout) inflater.inflate(R.layout.activity_main, null);
                MyWebView new_mywebview = rel_layout.findViewById(R.id.webview);

                //addView(new_mywebview);
                MyWebView old_mywebview = (MyWebView)view;
                MyWebView.webview_stack.push(new_mywebview);
                new_mywebview.WebViewInit(url, "", MyWebView.myCss);

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(new_mywebview);
                resultMsg.sendToTarget();
                new_mywebview.StartView();

                return true;
            }
        });
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            this.setWebContentsDebuggingEnabled(true);
        }
        //endregion

        WindowManager wm = (WindowManager)MainActivity.me.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        this.screen_width = display.getWidth();
        this.screen_height = display.getHeight();

        MyWebView.myJs = js;
        MyWebView.myCss = css;
        this.loadUrl(url);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void BindFresh(final SwipeRefreshLayout swipeRefresh)
    {
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
        {
            @Override
            public void onRefresh()
            {
                //重新加载刷新页面
                String url = MyWebView.this.getUrl();
                MyWebView.this.loadUrl(url);
            }
        });
        // swipeRefresh.post(new Runnable() {
        //     @Override
        //     public void run() {
        //         swipeRefresh.setRefreshing(true);
        //         //MyWebView.this.loadUrl(MyWebView.this.getUrl());
        //     }
        // });
        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_light,
                android.R.color.holo_red_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_green_light
        );

        this.setWebChromeClient(new WebChromeClient() // alert() 要用
        {
            public boolean onConsoleMessage(ConsoleMessage cm)
            {
                Log.d("MyApplication", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId());
                return true;
            }

            public void onProgressChanged(WebView view, int newProgress)
            {
                if (newProgress == 100)
                {
                    //隐藏进度条
                    swipeRefresh.setRefreshing(false);
                }
                else if (!swipeRefresh.isRefreshing())
                {
                    swipeRefresh.setRefreshing(true);
                }
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (swipe_refresh_layout == null)
        {
            return super.onTouchEvent(event);
        }
        float y = event.getY();
        float x = event.getX();
        if (x > this.screen_width/2 - x/6 && x < this.screen_width/2 + x/6)
        {
            swipe_refresh_layout.setEnabled(true);
        }
        else
        {
            swipe_refresh_layout.setEnabled(false);
        }
        return super.onTouchEvent(event);
    }

}


