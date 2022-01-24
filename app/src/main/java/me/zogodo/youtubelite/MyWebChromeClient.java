package me.zogodo.youtubelite;

import android.os.Message;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

public class MyWebChromeClient extends WebChromeClient
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
        MyWebView new_mywebview = new MyWebView(MainActivity.youtubeUrl);
        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(new_mywebview);
        resultMsg.sendToTarget();
        new_mywebview.StartView();
        return true;
    }
}
