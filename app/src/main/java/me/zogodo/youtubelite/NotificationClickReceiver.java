package me.zogodo.youtubelite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationClickReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("zzz", "notification action button clicked!");
        MyWebView wv = MyWebView.webview_stack.peek();
        wv.evaluateJavascript("javascript:movie_player.pauseVideo();", null);
    }
}
