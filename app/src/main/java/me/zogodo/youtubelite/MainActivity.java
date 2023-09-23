package me.zogodo.youtubelite;

import android.app.Notification;
//import android.app.Notification.MediaStyle;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

public class MainActivity extends AppCompatActivity
{
    public static MainActivity me;
    public static String indexUrl = "file:///android_asset/goto.html";
    public static WebView webView = null;
    long exitTime = 0;
    int notifyId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        MainActivity.me = this;

        Intent intent = getIntent();
        String dataString = intent.getDataString();
        if (dataString != null)
        {
            dataString = dataString.replaceAll("://z/", "://");
            indexUrl = dataString;
        }
        Log.e("zurl", indexUrl, null);

        webView = new MyWebView();
        webView.loadUrl(indexUrl);

        openRawMusicS();
    }

    private MediaPlayer mediaPlayer1;
    private void openRawMusicS() {
        mediaPlayer1 = MediaPlayer.create(this, R.raw.slience);
        mediaPlayer1.start();
        mediaPlayer1.setLooping(true);
        //mediaPlayer1.stop();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.e("noti", "onResume");
        cancelNotification(notifyId);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        Log.e("noti", "onPause");
        MyNotify();
    }

    private void MyMediaNotify()
    {
        Notification noti = new NotificationCompat.Builder(this, "aabbzz")
                .setSmallIcon(R.drawable.ic_youtube)
                .setContentTitle("Track title")
                .setContentText("Artist - Album")
                //.setLargeIcon(R.drawable.ic_youtube)
                .setStyle(new MediaStyle()
                    .setMediaSession(null))
                .build();

        /*
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(mContext, CHANNEL_ID);
        notificationBuilder
                .setStyle(
                        new MediaStyle()
                                .setMediaSession(token)
                                .setShowCancelButton(true)
                                .setCancelButtonIntent(
                                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                                                mContext, PlaybackStateCompat.ACTION_STOP)))
                .setColor(ContextCompat.getColor(mContext, R.color.notification_bg))
                .setSmallIcon(R.drawable.ic_stat_image_audiotrack)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setContentIntent(createContentIntent())
                .setContentTitle("Album")
                .setContentText("Artist")
                .setSubText("Song Name")
                .setLargeIcon(MusicLibrary.getAlbumBitmap(mContext, description.getMediaId()))
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(
                        mService, PlaybackStateCompat.ACTION_STOP));
        */
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
        bld.setSmallIcon(R.drawable.ic_youtube);
        bld.setContentTitle(title);
        bld.setPriority(NotificationManager.IMPORTANCE_MAX);
        bld.setCategory(Notification.CATEGORY_SERVICE);
        bld.setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));
        bld.addAction(R.drawable.ic_youtube, "Pause / Play", pit1);
        bld.setContentIntent(pit0);
        //bld.setCustomBigContentView();

        NotificationManager nm = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
            bld.setChannelId(channelId);
        }
        Notification ntf = bld.build();
        //ntf.contentIntent = pit0;  //same as bld.setContentIntent(pit)
        assert nm != null;
        nm.notify(notifyId, ntf);
    }

    public void cancelNotification(int notifyId)
    {
        NotificationManager nMgr = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        assert nMgr != null;
        nMgr.cancel(notifyId);
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        Log.e("a", "onNewIntent");
        super.onNewIntent(null);
    };

    @Override
    public void setContentView(View view)
    {
        if (view instanceof MyWebView)
        {
            webView = (MyWebView)view;
        }
        super.setContentView(view);
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

    @Override
    public void onStop()
    {
        Log.e("noti", "onStop");
        //cancelNotification(notifyId);
        super.onStop();
    }

    @Override
    public void onDestroy()
    {
        Log.e("noti", "onDestroy");
        cancelNotification(notifyId);
        super.onDestroy();
    }
}
