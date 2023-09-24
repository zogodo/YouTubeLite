package me.zogodo.youtubelite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.ValueCallback;

public class NotificationClickReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("noti", "notification action button clicked!");
        MyWebView wv = MyWebView.webview_stack.peek();
        ValueCallback cb = new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                Log.e("noti", "onReceiveValue " + value);
                MyNotification.ChangeSmallIcon(value);
            }
        };
        wv.evaluateJavascript("javascript:PauseOrPlay();", cb);
    }
}
