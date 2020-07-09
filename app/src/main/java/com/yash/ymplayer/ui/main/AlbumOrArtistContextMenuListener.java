package com.yash.ymplayer.ui.main;

import android.support.v4.media.MediaBrowserCompat;

public interface AlbumOrArtistContextMenuListener {
    enum ITEM_TYPE {ARTISTS, ALBUMS}

    void play(MediaBrowserCompat.MediaItem item, ITEM_TYPE type);

    void queueNext(MediaBrowserCompat.MediaItem item, ITEM_TYPE type);

    void addToPlaylist(MediaBrowserCompat.MediaItem item, String playlist, ITEM_TYPE type);
}
