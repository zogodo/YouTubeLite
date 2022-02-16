package me.zogodo.youtubelite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationClickReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent) {
        //todo 跳转之前要处理的逻辑
        Log.e("TAG", "userClick:我被点击啦！！！ ");
        //Intent newIntent = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //context.startActivity(newIntent);
    }
}
