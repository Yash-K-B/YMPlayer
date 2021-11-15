package com.yash.ymplayer.ui.main;

import android.support.v4.media.MediaBrowserCompat;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.recyclerview.widget.RecyclerView;

import com.yash.ymplayer.util.SongsListAdapter;

public interface SongContextMenuListener {
    void playSingle(MediaBrowserCompat.MediaItem item);

    void queueNext(MediaBrowserCompat.MediaItem item);

    void addToPlaylist(MediaBrowserCompat.MediaItem item, String playlist);

    void gotoAlbum(MediaBrowserCompat.MediaItem item);

    void gotoArtist(MediaBrowserCompat.MediaItem item);

    void shareSong(MediaBrowserCompat.MediaItem item);

    boolean deleteFromStorage(MediaBrowserCompat.MediaItem item, ActivityResultLauncher<IntentSenderRequest> launcher);
}
