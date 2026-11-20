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
import android.util.Log;
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
    private static KeepAliveService instance = null;
    private String mediaTitle = null;
    private String mediaChannel = null;
    private String mediaArtUrl = null;
    private String artLoadedUrl = null;   /*已加载完成的封面URL,避免重复下载*/
    private String artLoadingUrl = null;  /*正在下载的封面URL*/
    private android.graphics.Bitmap mediaArt = null;

    /*由 MainActivity 在切后台时读取页面标题/频道名后调用,更新媒体通知上的信息*/
    public static void updateMediaInfo(String title, String channel)
    {
        updateMediaInfo(title, channel, null);
    }

    /*artUrl: 视频封面图URL(可为null)*/
    public static void updateMediaInfo(String title, String channel, String artUrl)
    {
        KeepAliveService svc = instance;
        if (svc == null)
        {
            return;
        }
        svc.mediaTitle = title;
        svc.mediaChannel = channel;
        svc.mediaArtUrl = artUrl;
        svc.updateMediaMetadata();
        svc.refreshNotification();
        svc.downloadMediaArt(artUrl);
    }

    /*后台下载封面图,完成后更新元数据和通知(大图标)*/
    private void downloadMediaArt(final String url)
    {
        if (url == null || url.length() == 0)
        {
            return;
        }
        if (url.equals(artLoadedUrl) || url.equals(artLoadingUrl))
        {
            return;   /*已加载或正在加载*/
        }
        artLoadingUrl = url;
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                android.graphics.Bitmap bmp = null;
                int code = -1;
                try
                {
                    java.net.URL u = new java.net.URL(url);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) u.openConnection();
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setDoInput(true);
                    conn.setInstanceFollowRedirects(true);
                    /*ytimg 对没有 Referer/UA 的请求可能返回403(项目里保存图片的逻辑也加了这两个头)*/
                    conn.setRequestProperty("Referer", "https://www.youtube.com/");
                    conn.setRequestProperty("User-Agent",
                            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
                    conn.setRequestProperty("Accept", "image/*,*/*;q=0.8");
                    code = conn.getResponseCode();
                    if (code >= 200 && code < 300)
                    {
                        java.io.InputStream is = conn.getInputStream();
                        bmp = android.graphics.BitmapFactory.decodeStream(is);
                        is.close();
                    }
                    Log.e("media", "art download code=" + code + " url=" + url
                            + " bmp=" + (bmp == null ? "null" : (bmp.getWidth() + "x" + bmp.getHeight())));
                }
                catch (Exception e)
                {
                    Log.e("media", "art download err: " + e);
                    e.printStackTrace();
                }
                if (bmp == null && url != null && url.contains("hq720.jpg"))
                {
                    /*hq720 可能不存在(老视频),降级用 mqdefault(体积小且一定存在)*/
                    final String fallback = url.replace("hq720.jpg", "mqdefault.jpg");
                    new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                java.net.URL u2 = new java.net.URL(fallback);
                                java.net.HttpURLConnection c2 = (java.net.HttpURLConnection) u2.openConnection();
                                c2.setConnectTimeout(8000);
                                c2.setReadTimeout(8000);
                                c2.setDoInput(true);
                                c2.setInstanceFollowRedirects(true);
                                c2.setRequestProperty("Referer", "https://www.youtube.com/");
                                c2.setRequestProperty("User-Agent", "Mozilla/5.0");
                                android.graphics.Bitmap b2 = android.graphics.BitmapFactory.decodeStream(c2.getInputStream());
                                if (b2 != null)
                                {
                                    applyMediaArt(b2, fallback);
                                }
                            }
                            catch (Exception ex)
                            {
                                Log.e("media", "art fallback err: " + ex);
                            }
                            artLoadingUrl = null;
                        }
                    }).start();
                    return;
                }
                final android.graphics.Bitmap finalBmp = bmp;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (finalBmp != null)
                        {
                            applyMediaArt(finalBmp, url);
                        }
                        artLoadingUrl = null;
                    }
                });
            }
        }).start();
    }

    /*应用封面图:保存并刷新元数据+通知(切主线程执行)*/
    private void applyMediaArt(final android.graphics.Bitmap bmp, final String url)
    {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                mediaArt = bmp;
                artLoadedUrl = url;
                updateMediaMetadata();
                refreshNotification();
                Log.e("media", "art applied " + url);
            }
        });
    }

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
        instance = this;
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
        if (instance == this)
        {
            instance = null;
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
            /*用 startForeground 重新提交通知(而非 notify):
              keep住"前台服务通知"的身份,部分系统用 notify 更新时会丢失媒体卡片样式*/
            startForeground(NOTIFICATION_ID, buildNotification());
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
                    resumePlayback();
                }

                @Override
                public void onPause()
                {
                    pausePlayback();
                }
            });
            mediaSession.setActive(true);
            updatePlaybackState();
            updateMediaMetadata();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /*媒体元数据:标题 + 频道名(系统的媒体卡片显示的就是 title/artist)*/
    private void updateMediaMetadata()
    {
        if (mediaSession == null)
        {
            return;
        }
        try
        {
            MediaMetadata.Builder b = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE,
                            (mediaTitle != null && mediaTitle.length() > 0) ? mediaTitle : "正在后台播放");
            if (mediaChannel != null && mediaChannel.length() > 0)
            {
                b.putString(MediaMetadata.METADATA_KEY_ARTIST, mediaChannel);
            }
            if (mediaArt != null)
            {
                b.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, mediaArt);
                b.putBitmap(MediaMetadata.METADATA_KEY_ART, mediaArt);
            }
            mediaSession.setMetadata(b.build());
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
                    /*上报真实状态:系统的媒体卡片按钮据此显示"暂停"/"播放"*/
                    .setState(isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED,
                            PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
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
        String nTitle = (mediaTitle != null && mediaTitle.length() > 0) ? mediaTitle : "正在后台播放";
        String nText;
        if (mediaChannel != null && mediaChannel.length() > 0)
        {
            nText = mediaChannel;
        }
        else
        {
            nText = isPlaying ? "点击返回应用" : "已暂停,点击返回应用";
        }
        builder.setContentTitle(nTitle)
                .setContentText(nText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        if (mediaArt != null)
        {
            builder.setLargeIcon(mediaArt);   /*通知上的封面大图*/
        }

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
