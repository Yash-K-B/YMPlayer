package com.yash.ymplayer.ui.main;

import android.support.v4.media.MediaBrowserCompat;

public interface SongContextMenuListener {
    void playSingle(MediaBrowserCompat.MediaItem item);
    void queueNext(MediaBrowserCompat.MediaItem item);
    void addToPlaylist(MediaBrowserCompat.MediaItem item,String playlist);
    void gotoAlbum(MediaBrowserCompat.MediaItem item);
    void gotoArtist(MediaBrowserCompat.MediaItem item);
}
