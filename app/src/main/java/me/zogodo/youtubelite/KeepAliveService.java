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
import android.webkit.ValueCallback;
import android.webkit.WebView;

public class KeepAliveService extends Service
{
    public static final String CHANNEL_ID = "bg_playback";
    public static final int NOTIFICATION_ID = 1;
    public static final String ACTION_TOGGLE = "me.zogodo.youtubelite.action.TOGGLE";
    private static final String WAKE_LOCK_TAG = "youtubelite:bg_playback";
    private PowerManager.WakeLock wakeLock;
    private MediaSession mediaSession = null;
    private boolean isPlaying = false;

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
        buildMediaSession();
        startForeground(NOTIFICATION_ID, buildNotification());
        /*定时同步真实播放状态;也顺便决定是否持有唤醒锁*/
        scheduleStateSync();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction()))
        {
            togglePlayback();
        }
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

    //region 播放控制
    private void togglePlayback()
    {
        if (isPlaying)
        {
            pausePlayback();
        }
        else
        {
            resumePlayback();
        }
    }

    private void pausePlayback()
    {
        /*同时把 __keepPlaying 置 false,否则 myjs.js 的"被打断自动续播"会立刻把它恢复播放*/
        runJs("javascript:(function(){var p=document.getElementById('movie_player');" +
                "if(p&&typeof p.pauseVideo==='function'){p.pauseVideo();}" +
                "window.__keepPlaying=false;})()");
        setPlayingState(false);
    }

    private void resumePlayback()
    {
        runJs("javascript:(function(){var p=document.getElementById('movie_player');" +
                "if(p&&typeof p.playVideo==='function'){p.playVideo();}" +
                "window.__keepPlaying=true;})()");
        setPlayingState(true);
    }

    private void runJs(String js)
    {
        try
        {
            WebView wv = MainActivity.webView;
            if (wv != null)
            {
                wv.evaluateJavascript(js, null);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*更新状态:通知按钮/文字 + 唤醒锁。注意 MediaSession 的播放状态始终保持 PLAYING,
      否则部分系统(尤其国产ROM)会把媒体通知降级成普通通知*/
    private void setPlayingState(boolean playing)
    {
        isPlaying = playing;
        updateWakeLock();
        updatePlaybackState();
        refreshNotification();
    }

    private void updateWakeLock()
    {
        if (wakeLock == null)
        {
            return;
        }
        try
        {
            if (isPlaying && !wakeLock.isHeld())
            {
                wakeLock.acquire();
            }
            else if (!isPlaying && wakeLock.isHeld())
            {
                wakeLock.release();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void refreshNotification()
    {
        try
        {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.notify(NOTIFICATION_ID, buildNotification());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*每2秒查询一次视频真实状态,与通知状态保持一致(覆盖在app内暂停/播完等情况)*/
    private void scheduleStateSync()
    {
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            @Override
            public void run()
            {
                WebView wv = MainActivity.webView;
                if (wv != null)
                {
                    try
                    {
                        wv.evaluateJavascript("javascript:IsVideoPlaying();", new ValueCallback<String>()
                        {
                            @Override
                            public void onReceiveValue(String value)
                            {
                                boolean playing = "true".equals(value);
                                if (playing != isPlaying)
                                {
                                    setPlayingState(playing);
                                }
                            }
                        });
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
                handler.postDelayed(this, 2000);
            }
        }, 2000);
    }
    //endregion

    //region MediaSession
    /*媒体通知绑定 MediaSession;始终处于"播放中"状态以保证永远是媒体通知,
      提供播放/暂停能力,点击通知/媒体卡片回到app*/
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
            mediaSession.setCallback(new MediaSession.Callback()
            {
                @Override
                public void onPlay()
                {
                    togglePlayback();
                }

                @Override
                public void onPause()
                {
                    togglePlayback();
                }
            });
            mediaSession.setActive(true);
            updatePlaybackState();
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

    private void updatePlaybackState()
    {
        if (mediaSession == null)
        {
            return;
        }
        try
        {
            PlaybackState state = new PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE
                            | PlaybackState.ACTION_PLAY_PAUSE)
                    /*始终报告 PLAYING:暂停时也不降级,保持媒体通知形态*/
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                    .build();
            mediaSession.setPlaybackState(state);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    //endregion

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

        /*播放/暂停按钮:发送一个动作给本服务来处理*/
        Intent toggleIntent = new Intent(this, KeepAliveService.class);
        toggleIntent.setAction(ACTION_TOGGLE);
        PendingIntent togglePi = PendingIntent.getService(this, 1, toggleIntent,
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
                .setContentText(isPlaying ? "点击返回应用" : "已暂停,点击返回应用")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        Notification.Action action = new Notification.Action.Builder(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                isPlaying ? "暂停" : "播放",
                togglePi).build();
        builder.addAction(action);

        /*媒体样式:需要 MediaSession token,并指定折叠时显示第0个按钮*/
        if (mediaSession != null)
        {
            builder.setStyle(new Notification.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0));
        }
        return builder.build();
    }
}
