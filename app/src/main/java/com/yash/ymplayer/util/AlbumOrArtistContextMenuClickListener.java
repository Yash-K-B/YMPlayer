package com.yash.ymplayer.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.widget.Toast;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.storage.AudioProvider;
import com.yash.ymplayer.storage.OfflineMediaProvider;
import com.yash.ymplayer.ui.main.AlbumOrArtistContextMenuListener;

import java.util.List;

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

        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME}, MediaStore.Audio.Playlists.NAME + " = '" + playlist + "'", null, null);
        cursor.moveToFirst();
        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
        cursor.close();
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", id);
        Cursor cur = resolver.query(uri, new String[]{MediaStore.Audio.Playlists.Members.PLAY_ORDER}, null, null, null);
        cur.moveToLast();
        int base = cur.getCount() == 0 ? -1 : cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER));
        cur.close();
        if(type == ITEM_TYPE.ALBUMS){
           List<MediaBrowserCompat.MediaItem> songs =  Repository.getInstance(context).getSongsOfAlbum(item.getMediaId());
           for(MediaBrowserCompat.MediaItem song:songs){
               String[] parts = song.getMediaId().split("[/|]");
               ContentValues values = new ContentValues();
               values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, ++base);
               values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, parts[parts.length - 1]);
               resolver.insert(uri, values);
           }
        } else if(type == ITEM_TYPE.ARTISTS){
            List<MediaBrowserCompat.MediaItem> songs =  Repository.getInstance(context).getSongsOfArtist(item.getMediaId());
            for(MediaBrowserCompat.MediaItem song:songs){
                String[] parts = song.getMediaId().split("[/|]");
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, ++base);
                values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, parts[parts.length - 1]);
                resolver.insert(uri, values);
            }
        }
        Toast.makeText(context, "Added to "+playlist, Toast.LENGTH_SHORT).show();

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
