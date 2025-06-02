package me.zogodo.youtubelite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.session.MediaSession;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

public class MyNotification
{
    public static int notifyId = 0;
    public static NotificationCompat.Builder bld;
    public static NotificationManager nm;
    public static PendingIntent pit1;

    public static void MyMediaNotify(String title, String contentText, String iconUrl)
    {
        MediaSessionCompat mediaSession = new MediaSessionCompat(MainActivity.me, "PlayerService");

        androidx.media.app.NotificationCompat.MediaStyle mediaStyle = new androidx.media.app.NotificationCompat.MediaStyle();
        mediaStyle.setShowActionsInCompactView(0); //折叠后还显示第一个icon 如果要显示多个可以写 (0, 1) 最多三个
        mediaStyle.setMediaSession(mediaSession.getSessionToken());

        Intent it0 = new Intent(MainActivity.me, MainActivity.class);
        PendingIntent pit0 = PendingIntent.getActivity(MainActivity.me, 0, it0, 0);

        Intent it1 = new Intent(MainActivity.me, NotificationClickReceiver.class);
        pit1 = PendingIntent.getBroadcast(MainActivity.me,  0, it1, PendingIntent.FLAG_UPDATE_CURRENT);

        bld = new NotificationCompat.Builder(MainActivity.me, "channel_id");
        bld.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        bld.setSmallIcon(R.drawable.ic_youtube);
        //bld.addAction(R.drawable.ic_prev, "Previous", prevPendingIntent)
        bld.addAction(R.drawable.ic_pause, "Pause", pit1);
        //bld.addAction(R.drawable.ic_next, "Next", nextPendingIntent)
        bld.setStyle(mediaStyle);
        bld.setContentTitle(title);
        bld.setContentText(contentText);
        Bitmap bm = BitmapFactory.decodeResource(MainActivity.me.getResources(), R.drawable.ic_youtube);
        bld.setLargeIcon(bm); //没用?
        bld.setContentIntent(pit0);

        Notification notification = bld.build();

        nm = (NotificationManager)MainActivity.me.getSystemService(Context.NOTIFICATION_SERVICE);
        assert nm != null;
        nm.notify(notifyId, notification);
    }

    public static void ChangeSmallIcon(String re)
    {
        Log.e("noti", "ChangeSmallIcon " + re);
        int icon = R.drawable.ic_play;
        if (re.equals("1")) icon = R.drawable.ic_pause;

        //bld.clearActions();
        bld.addAction(icon, "Play", pit1);
        nm.notify(notifyId, bld.build());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void MyMediaNotify_O()
    {
        MediaSession mediaSession = new MediaSession(MainActivity.me, "PlayerService");

        Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
        mediaStyle.setShowActionsInCompactView(0);  //折叠后还显示第一个icon 如果要显示多个可以写 (0, 1) 最多三个
        mediaStyle.setMediaSession(mediaSession.getSessionToken());

        Intent it0 = new Intent(MainActivity.me, MainActivity.class);
        PendingIntent pit0 = PendingIntent.getActivity(MainActivity.me, 0, it0, 0);

        Intent it1 = new Intent(MainActivity.me, NotificationClickReceiver.class);
        PendingIntent pit1 = PendingIntent.getBroadcast(MainActivity.me,  0, it1, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder bld1 = new Notification.Builder(MainActivity.me, "channel_id");
        bld1.setStyle(mediaStyle);
        bld1.setContentTitle("MyMediaNotify");
        bld1.setSmallIcon(R.drawable.ic_youtube);
        bld1.addAction(R.drawable.ic_pause, "Pause", pit1);
        bld1.setContentIntent(pit0);

        //Icon ic = Icon.createWithResource(MainActivity.me, R.drawable.ic_pause);
        //Notification.Action action = new Notification.Action.Builder(ic, "Pause", pit1).build();
        //bld1.addAction(action);

        Notification notification = bld1.build();

        NotificationManager nm = (NotificationManager)MainActivity.me.getSystemService(Context.NOTIFICATION_SERVICE);
        assert nm != null;
        nm.notify(notifyId, notification);
    }

    public static void MyNotify()
    {
        String channelId = "channel_id";
        String title = "Running in the background";

        Intent it0 = new Intent(MainActivity.me, MainActivity.class);
        PendingIntent pit0 = PendingIntent.getActivity(MainActivity.me, 0, it0, 0);

        Intent it1 = new Intent(MainActivity.me, NotificationClickReceiver.class);
        PendingIntent pit1 = PendingIntent.getBroadcast(MainActivity.me,  0, it1, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder bld = new NotificationCompat.Builder(MainActivity.me, channelId);
        bld.setOngoing(true);
        bld.setSmallIcon(R.drawable.ic_youtube);
        bld.setContentTitle(title);
        bld.setPriority(NotificationManager.IMPORTANCE_MAX);
        bld.setCategory(Notification.CATEGORY_SERVICE);
        bld.setColor(ContextCompat.getColor(MainActivity.me, R.color.colorPrimaryDark));
        bld.addAction(R.drawable.ic_youtube, "Pause / Play", pit1);
        bld.setContentIntent(pit0);
        //bld.setCustomBigContentView();

        NotificationManager nm = (NotificationManager)MainActivity.me.getSystemService(Context.NOTIFICATION_SERVICE);

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

    public static void CancelNotification(int notifyId)
    {
        NotificationManager nMgr = (NotificationManager) MainActivity.me.getSystemService(Context.NOTIFICATION_SERVICE);
        assert nMgr != null;
        nMgr.cancel(notifyId);
    }

}
