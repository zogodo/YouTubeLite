package me.zogodo.youtubelite;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.List;

public class MyMediaBrowserServiceCompat extends MediaBrowserServiceCompat
{
    private static final String MY_MEDIA_ROOT_ID = "media_root_id";
    private static final String MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mediaSession;
    private PlaybackStateCompat.Builder stateBuilder;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a MediaSessionCompat
        mediaSession = new MediaSessionCompat(this, "channelId");

        // Enable callbacks from MediaButtons and TransportControls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mediaSession.setPlaybackState(stateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mediaSession.setCallback(new MySessionCallback());

        // Set the session's token so that client activities can communicate with it.
        setSessionToken(mediaSession.getSessionToken());
        // Given a media session and its context (usually the component containing the session)
        // Create a NotificationCompat.Builder

        // Get the session's metadata

        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        //MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channelId");

        Bitmap icon = BitmapFactory.decodeResource(this.getResources(),
                R.drawable.ic_launcher_foreground);

        builder
            // Add the metadata for the currently playing track
            //.setContentTitle(description.getTitle())
            //.setContentText(description.getSubtitle())
            //.setSubText(description.getDescription())
            //.setLargeIcon(description.getIconBitmap())
            .setContentTitle("description.getTitle()")
            .setContentText("description.getSubtitle()")
            .setSubText("description.getDescription()")
            .setLargeIcon(icon)

            // Enable launching the player by clicking the notification
            .setContentIntent(controller.getSessionActivity())

            // Stop the service when the notification is swiped away
            .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                    PlaybackStateCompat.ACTION_STOP))

            // Make the transport controls visible on the lockscreen
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            // Add an app icon and set its accent color
            // Be careful about the color
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setColor(ContextCompat.getColor(this, R.color.colorAccent))

            // Add a pause button
            .addAction(new NotificationCompat.Action(
                    R.drawable.ic_launcher_foreground, getString(R.string.app_name),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE)))

            // Take advantage of MediaStyle features
            .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.getSessionToken())
                    .setShowActionsInCompactView(0)

                    // Add a cancel button
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                            PlaybackStateCompat.ACTION_STOP)));

        NotificationChannel chan = new NotificationChannel(
                "MyChannelId",
                "My Foreground Service",
                NotificationManager.IMPORTANCE_LOW);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_SECRET);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        // Display the notification and place the service in the foreground
        //startForeground(1, builder.build());

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(
                this, "MyChannelId");
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("App is running on foreground")
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setChannelId("MyChannelId")
                .setColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                // Add a pause button
                .addAction(new NotificationCompat.Action(
                        R.drawable.ic_launcher_foreground, "Stop Play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                /*
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0)
                        // Add a cancel button
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this,
                                PlaybackStateCompat.ACTION_STOP)))
                */
                .build();

        startForeground(1, notification);
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {

        // (Optional) Control the level of access for the specified package name.
        // You'll need to write your own logic to do this.
        if (clientPackageName.equals(clientUid)) {
            // Returns a root ID that clients can use with onLoadChildren() to retrieve
            // the content hierarchy.
            return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
        } else {
            // Clients can connect, but this BrowserRoot is an empty hierachy
            // so onLoadChildren returns nothing. This disables the ability to browse for content.
            return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
        }
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result)
    {
        return;
    }
}
