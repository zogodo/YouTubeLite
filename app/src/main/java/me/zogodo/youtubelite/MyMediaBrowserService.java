package me.zogodo.youtubelite;

import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class MyMediaBrowserService extends MediaBrowserService
{

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints)
    {
        String MY_RECENTS_ROOT_ID = "MY_RECENTS_ROOT_ID";
        String MY_MEDIA_ROOT_ID   = "MY_MEDIA_ROOT_ID";
        String MY_EMPTY_ROOT_ID   = "MY_EMPTY_ROOT_ID";
        // Verify that the specified package is SystemUI. You'll need to write your
        // own logic to do this.
        if (!clientPackageName.endsWith(Integer.toString(clientUid))) {
            if (rootHints != null) {
                if (rootHints.getBoolean(BrowserRoot.EXTRA_RECENT)) {
                    // Return a tree with a single playable media item for resumption.
                    Bundle extras = new Bundle();
                    extras.putBoolean(BrowserRoot.EXTRA_RECENT, true);
                    return new BrowserRoot(MY_RECENTS_ROOT_ID, extras);
                }
            }
            // You can return your normal tree if the EXTRA_RECENT flag is not present.
            return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
        }
        // Return an empty tree to disallow browsing.
        return new BrowserRoot(MY_EMPTY_ROOT_ID, null);
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull Result<List<MediaBrowser.MediaItem>> result)
    {

    }
}
