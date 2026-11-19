package me.zogodo.youtubelite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

public class KeepAliveService extends Service
{
    public static final String CHANNEL_ID = "bg_playback";
    public static final int NOTIFICATION_ID = 1;
    private static final String WAKE_LOCK_TAG = "youtubelite:bg_playback";
    private PowerManager.WakeLock wakeLock;
    private MediaSession mediaSession = null;

    public static void start(Context ctx)
    {
        Intent intent = new Intent(ctx, KeepAliveService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            ctx.startForegroundService(intent);
        }
        else
        {
            ctx.startService(intent);
        }
    }

    public static void stop(Context ctx)
    {
        ctx.stopService(new Intent(ctx, KeepAliveService.class));
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        createChannel();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
        buildMediaSession();
        startForeground(NOTIFICATION_ID, buildNotification());
    }

    /*媒体通知必须绑定一个 MediaSession;不提供任何 action(通知里没有播放/暂停等按钮),
      点通知或系统的媒体卡片都通过 contentIntent / sessionActivity 回到 app*/
    private void buildMediaSession()
    {
        try
        {
            Intent backIntent = new Intent(this, MainActivity.class);
            backIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(this, 0, backIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            mediaSession = new MediaSession(this, "youtubelite");
            mediaSession.setSessionActivity(pi);
            mediaSession.setActive(true);
            /*没有任何可执行 action,所以通知/媒体卡片上不会出现控制按钮*/
            PlaybackState state = new PlaybackState.Builder()
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .build();
            mediaSession.setPlaybackState(state);
            try
            {
                mediaSession.setMetadata(new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_TITLE, "正在后台播放")
                        .build());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        if (wakeLock != null && wakeLock.isHeld())
        {
            wakeLock.release();
        }
        if (mediaSession != null)
        {
            try
            {
                mediaSession.setActive(false);
                mediaSession.release();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            mediaSession = null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    private void createChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "后台播放",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification()
    {
        Intent contentIntent = new Intent(this, MainActivity.class);
        contentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            builder = new Notification.Builder(this, CHANNEL_ID);
        }
        else
        {
            builder = new Notification.Builder(this);
        }
        builder.setContentTitle("正在后台播放")
                .setContentText("点击返回应用")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        /*媒体样式通知:需要 MediaSession token 才会被系统当作媒体通知展示*/
        if (mediaSession != null)
        {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken()));
        }
        return builder.build();
    }
}
