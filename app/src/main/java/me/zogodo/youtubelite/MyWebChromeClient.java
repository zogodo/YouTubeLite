package me.zogodo.youtubelite;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.FrameLayout;

public class MyWebChromeClient extends WebChromeClient
{
    private View mCustomView;
    private WebChromeClient.CustomViewCallback mCustomViewCallback;
    protected FrameLayout mFullscreenContainer;
    private int mOriginalOrientation;
    private int mOriginalSystemUiVisibility;

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
        MyWebView new_mywebview = new MyWebView(MainActivity.youtubeUrl);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(new_mywebview);
        resultMsg.sendToTarget();
        new_mywebview.StartView();
        return true;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onShowCustomView(View view, CustomViewCallback callback) {
        MainActivity.me.setContentView(view);
        MainActivity.me.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        Window win = MainActivity.me.getWindow();
        WindowManager.LayoutParams attrs = win.getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        win.setAttributes(attrs);
        win.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        //win.getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        //win.getDecorView().setSystemUiVisibility(
        //        View.SYSTEM_UI_FLAG_LOW_PROFILE
        //        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        //        | View.SYSTEM_UI_FLAG_FULLSCREEN
        //        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        super.onShowCustomView(view, callback);
    }

    @Override
    public void onHideCustomView() {
        MainActivity.me.setContentView(MyWebView.webview_stack.peek());
        MainActivity.me.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        Window win = MainActivity.me.getWindow();
        WindowManager.LayoutParams attrs = win.getAttributes();
        attrs.flags &= (~WindowManager.LayoutParams.FLAG_FULLSCREEN);
        win.setAttributes(attrs);
        win.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        super.onHideCustomView();
    }
}
