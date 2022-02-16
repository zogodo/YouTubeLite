package me.zogodo.youtubelite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;
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
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, "MyChannelId");
        Intent ii = new Intent(this.getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, ii, 0);
        NotificationCompat.Builder mBuilder = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("YouTube is playing in the background")
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setChannelId("MyChannelId")
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                //.setContentIntent(conPendingIntent)
                // Add a pause button
                .addAction(new NotificationCompat.Action(R.drawable.youtube, "Stop", pendingIntent))
                .addAction(new NotificationCompat.Action(R.drawable.youtube, "Start", pendingIntent));

        NotificationManager mNotificationManager =
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        // === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            String channelId = "Your_channel_id";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }
        Notification notification = mBuilder.build();
        notification.contentIntent = pendingIntent;
        mNotificationManager.notify(0, notification);
    }

    public void cancelNotification(int notifyId)
    {
        NotificationManager nMgr = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancel(notifyId);
    }

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
