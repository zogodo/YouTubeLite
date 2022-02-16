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
        String title = "Playing in the background";

        Intent it = new Intent(this, MainActivity.class);
        //it.setAction("zgd");
        //it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pit = PendingIntent.getActivity(this, 0, it, 0);

        // for notification itself
        //Intent notificationIntent = (new Intent()).setAction("zgd");
        //PendingIntent notificationPendingIntent = PendingIntent.getBroadcast(this, NOTIFICATION_INTENT_REQUEST_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // for action button
        Intent actionIntent = new Intent(this, NotificationClickReceiver.class);
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(this,  0, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        // execute onNotificationClick
        //mNotificationBuilder.setContentIntent(notificationPendingIntent);

        // execute onButtonClick
        //mNotificationBuilder.addAction(R.drawable.button_icon, BUTTON_TEXT, actionPendingIntent);

        NotificationCompat.Builder bld = new NotificationCompat.Builder(this, "MyChannelId");
        bld.setOngoing(true);
        bld.setSmallIcon(R.mipmap.ic_launcher);
        bld.setContentTitle(title);
        bld.setPriority(NotificationManager.IMPORTANCE_MAX);
        bld.setCategory(Notification.CATEGORY_SERVICE);
        bld.setChannelId(channelId);
        bld.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        bld.addAction(R.drawable.youtube, "Pause", actionPendingIntent);
        //bld.addAction(R.drawable.youtube, "Start", pit);
        //bld.setContentIntent(pit);
        //@SuppressLint("RestrictedApi") RemoteViews rv = bld.getContentView();
        //rv.setOnClickPendingIntent(0, pit);

        NotificationManager nm = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
            bld.setChannelId(channelId);
        }
        Notification ntf = bld.build();
        ntf.contentIntent = pit; //bld.setContentIntent(pit)
        nm.notify(0, ntf);

        //IntentFilter filter = new IntentFilter();
        //filter.addAction("zgd");
        //NotificationClickReceiver mReceiver = new NotificationClickReceiver();
        //registerReceiver(mReceiver, filter);
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
