package me.zogodo.youtubelite;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity
{
    public static MainActivity me;
    public static String indexUrl = "https://m.youtube.com";
    WebView webView = null;
    long exitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        MainActivity.me = this;

        //indexUrl = "http://nsfwyoutube.com/";
        Intent intent = getIntent();
        String dataString = intent.getDataString();
        if (dataString != null)
        {
            indexUrl = dataString;
        }

        webView = new MyWebView();
        webView.loadUrl(indexUrl);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        cancelNotification(0);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        MyNotify();
    }

    private void MyNotify()
    {
        String channelId = "channel_id";
        String title = "Running in the background";

        Intent it0 = new Intent(this, MainActivity.class);
        PendingIntent pit0 = PendingIntent.getActivity(this, 0, it0, 0);

        Intent it1 = new Intent(this, NotificationClickReceiver.class);
        PendingIntent pit1 = PendingIntent.getBroadcast(this,  0, it1, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder bld = new NotificationCompat.Builder(this, channelId);
        bld.setOngoing(true);
        bld.setSmallIcon(R.mipmap.ic_launcher);
        bld.setContentTitle(title);
        bld.setPriority(NotificationManager.IMPORTANCE_MAX);
        bld.setCategory(Notification.CATEGORY_SERVICE);
        bld.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        bld.addAction(R.drawable.youtube, "Pause / Play", pit1);
        bld.setContentIntent(pit0);

        NotificationManager nm = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
            bld.setChannelId(channelId);
        }
        Notification ntf = bld.build();
        //ntf.contentIntent = pit0;  //same as bld.setContentIntent(pit)
        nm.notify(0, ntf);
    }

    public void cancelNotification(int notifyId)
    {
        NotificationManager nMgr = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(notifyId);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Log.e("a", "b");
        super.onNewIntent(null);
    };

    public void onBackPressed()
    {
        if (webView != null && webView.canGoBack())
        {
            webView.goBack();
        }
        else
        {
            // 判断是否可后退，是则后退，否则退出程序
            if (((System.currentTimeMillis() - exitTime) > 3000))
            {
                Toast.makeText(getApplicationContext(), "再按一次返回 退出程序", Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            }
            else
            {
                finish();
                System.exit(0);
            }
        }
    }
}
