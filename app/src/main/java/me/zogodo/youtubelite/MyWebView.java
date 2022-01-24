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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.RelativeLayout;

import java.util.Stack;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * Created by zogod on 17/2/19.
 */
public class MyWebView extends WebView
{
    //region 共有变量
    public Activity activity;
    public Stack<MyWebView> webview_stack;
    public boolean first_view = true;
    public String myjs = "";
    public String mycss = "";
    public SwipeRefreshLayout swipe_refresh_layout = null;
    public int screen_witdh;
    public int screen_height;
    //endregion

    //region 构造器
    public MyWebView(Activity activity, Stack<MyWebView> webview_stack)
    {
        super(activity);
        this.activity = activity;
        this.webview_stack = webview_stack;
        this.webview_stack.push(this);

        this.setVisibility(this.INVISIBLE);
    }
    public MyWebView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.activity = (Activity)context;
    }
    //endregion

    //region goBack
    public boolean canGoBack()
    {
        return this.webview_stack.size() > 1;
    }
    public void goBack()
    {
        this.webview_stack.pop();
        MyWebView old_mywebview = this.webview_stack.peek();

        old_mywebview.StartView();
    }
    //endregion

    public void StartView()
    {
        this.setVisibility(VISIBLE);
        Object ob = this.getParent();
        if (ob != null)
        {
            this.swipe_refresh_layout = (SwipeRefreshLayout)ob;
            this.BindFresh(this.swipe_refresh_layout);
            ConstraintLayout rel_layout = (ConstraintLayout)this.swipe_refresh_layout.getParent();
            this.activity.setContentView(rel_layout);
        }
        else
        {
            this.activity.setContentView(this);
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
                "style.innerHTML = '" + this.mycss + "';" +
                "parent.appendChild(style)" +
                "})()");
    }

    public void VebViewInit(String url, String js, String css)
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
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                //WebView newWebView = new WebView(getContext());
                //addView(newWebView);
                //WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                //transport.setWebView(newWebView);
                //resultMsg.sendToTarget();

                //WebView wvMain = new WebView(getContext());
                //wvMain.setVerticalScrollBarEnabled(false);
                //wvMain.setHorizontalScrollBarEnabled(false);
                //wvMain.setWebViewClient(new UriWebViewClient());
                //WebSettings webSettings = wvMain.getSettings();
                //webSettings.setJavaScriptEnabled(true);
                //webSettings.setAppCacheEnabled(true);
                //webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
                //webSettings.setSupportMultipleWindows(true);
                //wvMain.setLayoutParams(new FrameLayout.LayoutParams(
                //        ViewGroup.LayoutParams.MATCH_PARENT,
                //        ViewGroup.LayoutParams.MATCH_PARENT));
                //mContainer.addView(wvMain);
                //WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                //transport.setWebView(wvMain);
                //resultMsg.sendToTarget();

                //s//MyWebView new_mywebview = new MyWebView(old_mywebview.activity, old_mywebview.webview_stack);

                LayoutInflater inflater = (LayoutInflater) MyWebView.this.activity.getSystemService(LAYOUT_INFLATER_SERVICE);
                ConstraintLayout rel_layout = (ConstraintLayout) inflater.inflate(R.layout.activity_main, null);
                MyWebView new_mywebview = rel_layout.findViewById(R.id.webview);

                //addView(new_mywebview);
                MyWebView old_mywebview = (MyWebView)view;
                old_mywebview.webview_stack.push(new_mywebview);
                new_mywebview.activity = old_mywebview.activity;
                new_mywebview.webview_stack = old_mywebview.webview_stack;
                new_mywebview.VebViewInit(url, old_mywebview.myjs, old_mywebview.mycss);
                old_mywebview.webview_stack.push(new_mywebview);

                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(new_mywebview);
                resultMsg.sendToTarget();
                new_mywebview.VebViewInit(MyWebViewClient.newUrl, "", "");
                new_mywebview.StartView();

                return true;
            }
        });
        if (BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            this.setWebContentsDebuggingEnabled(true);
        }
        //endregion

        WindowManager wm = (WindowManager)this.activity.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        this.screen_witdh = display.getWidth();
        this.screen_height = display.getHeight();

        this.myjs = js;
        this.mycss = css;
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
        if (x > this.screen_witdh/2 - x/6 && x < this.screen_witdh/2 + x/6)
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


