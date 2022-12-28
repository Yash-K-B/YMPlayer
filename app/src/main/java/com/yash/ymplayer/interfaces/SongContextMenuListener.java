package com.yash.ymplayer.interfaces;

import android.support.v4.media.MediaBrowserCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;

import com.yash.ymplayer.util.Keys;

public interface SongContextMenuListener {
    void playSingle(MediaBrowserCompat.MediaItem item);

    void queueNext(MediaBrowserCompat.MediaItem item);

    void queueLast(MediaBrowserCompat.MediaItem item);

    void addToPlaylist(MediaBrowserCompat.MediaItem item, String playlist, Keys.PlaylistType playlistType);

    void gotoAlbum(MediaBrowserCompat.MediaItem item);

    void gotoArtist(MediaBrowserCompat.MediaItem item);

    void shareSong(MediaBrowserCompat.MediaItem item);

    boolean deleteFromStorage(MediaBrowserCompat.MediaItem item, ActivityResultLauncher<IntentSenderRequest> launcher);
}
