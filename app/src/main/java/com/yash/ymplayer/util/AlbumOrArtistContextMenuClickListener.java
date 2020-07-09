package com.yash.ymplayer.util;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import com.yash.ymplayer.helper.LogHelper;
import com.yash.ymplayer.storage.AudioProvider;
import com.yash.ymplayer.storage.OfflineMediaProvider;
import com.yash.ymplayer.ui.main.AlbumOrArtistContextMenuListener;

import static com.yash.ymplayer.storage.OfflineMediaProvider.*;

public class AlbumOrArtistContextMenuClickListener implements AlbumOrArtistContextMenuListener {
    private static final String TAG = "AlbumOrArtistContextMen";
    MediaControllerCompat mMediaController;
    Context context;

    public AlbumOrArtistContextMenuClickListener(Context context, MediaControllerCompat mMediaController) {
        this.mMediaController = mMediaController;
        this.context = context;
    }

    @Override
    public void play(MediaBrowserCompat.MediaItem item, ITEM_TYPE type) {
        try {
            if (item.getMediaId() == null)
                throw new AlbumOrArtistContextMenuClickException("getting null from play() method");

            mMediaController.getTransportControls().playFromMediaId(item.getMediaId(), null);
            LogHelper.d(TAG, "AlbumOrArtistContextMenuClickListener play: " + item.getMediaId());

        } catch (AlbumOrArtistContextMenuClickException e) {
            e.log();
        }

    }

    @Override
    public void queueNext(MediaBrowserCompat.MediaItem item, ITEM_TYPE type) {
        try {
            if (item.getMediaId() == null)
                throw new AlbumOrArtistContextMenuClickException("getting null from queueNext() method");

//            String mediaId = getFirstToken(type) + item.getMediaId();
            Bundle extras = new Bundle();
            extras.putString(Keys.MEDIA_ID, item.getMediaId());
            extras.putInt(Keys.QUEUE_HINT, type == ITEM_TYPE.ARTISTS ? AudioProvider.QueueHint.ARTIST_SONGS : AudioProvider.QueueHint.ALBUM_SONGS);
            mMediaController.getTransportControls().sendCustomAction(Keys.Action.QUEUE_NEXT, extras);
            LogHelper.d(TAG, "AlbumOrArtistContextMenuClickListener queueNext: " + item.getMediaId());

        } catch (AlbumOrArtistContextMenuClickException e) {
            e.log();
        }
    }

    @Override
    public void addToPlaylist(MediaBrowserCompat.MediaItem item, String playlist, ITEM_TYPE type) {

    }

    String getFirstToken(ITEM_TYPE type) {
        switch (type) {
            case ALBUMS:
                return "ALBUMS/";
            case ARTISTS:
                return "ARTISTS/";
            default:
                return "";
        }
    }

    static class AlbumOrArtistContextMenuClickException extends Exception {
        String str;

        public AlbumOrArtistContextMenuClickException(String message) {
            super(message);
            this.str = message;
        }

        void log() {
            LogHelper.d(TAG, "log: " + str);
        }
    }
}
