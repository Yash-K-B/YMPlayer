package com.yash.ymplayer.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;

import com.yash.ymplayer.ListExpandActivity;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.storage.AudioProvider;
import com.yash.ymplayer.storage.OfflineMediaProvider;
import com.yash.ymplayer.ui.main.Playlists;
import com.yash.ymplayer.ui.main.SongContextMenuListener;
//import androidx.activity.result.IntentSenderRequest;

import java.io.File;
import java.util.ArrayList;

public class SongsContextMenuClickListener implements SongContextMenuListener {
    private static final String TAG = "SongsContextMenuClickLi";
    MediaControllerCompat mMediaController;
    Context context;

    public SongsContextMenuClickListener(Context context, MediaControllerCompat mMediaController) {
        this.mMediaController = mMediaController;
        this.context = context;
    }

    @Override
    public void playSingle(MediaBrowserCompat.MediaItem item) {
        Bundle extra = new Bundle();
        extra.putBoolean(Keys.PLAY_SINGLE, true);
        mMediaController.getTransportControls().playFromMediaId(item.getDescription().getMediaId(), extra);
    }

    @Override
    public void queueNext(MediaBrowserCompat.MediaItem item) {
        Bundle extras = new Bundle();
        extras.putString(Keys.MEDIA_ID, item.getDescription().getMediaId());
        extras.putInt(Keys.QUEUE_HINT, AudioProvider.QueueHint.SINGLE_SONG);
        mMediaController.getTransportControls().sendCustomAction(Keys.Action.QUEUE_NEXT, extras);
    }

    @Override
    public void addToPlaylist(MediaBrowserCompat.MediaItem item, String playlist) {
        String[] parts = item.getDescription().getMediaId().split("[/|]");
        LogHelper.d(TAG, "addToPlaylist: id:" + parts[parts.length - 1]);
//        Bundle extras = new Bundle();
//        extras.putString(Keys.MEDIA_ID, parts[parts.length - 1]);
//        extras.putString(Keys.TITLE, item.getDescription().getTitle().toString());
//        extras.putString(Keys.ARTIST, item.getDescription().getSubtitle().toString());
//        extras.putString(Keys.ALBUM, item.getDescription().getDescription().toString());
//        extras.putString(Keys.PLAYLIST_NAME, playlist);
//        mMediaController.getTransportControls().sendCustomAction(Keys.Action.ADD_TO_PLAYLIST, extras);

        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME}, MediaStore.Audio.Playlists.NAME + " = '" + playlist + "'", null, null);
        cursor.moveToFirst();
        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
        cursor.close();
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", id);
        Cursor cur = resolver.query(uri, new String[]{MediaStore.Audio.Playlists.Members.PLAY_ORDER}, null, null, null);
        cur.moveToLast();
        final int base = cur.getCount() == 0 ? -1 : cur.getInt(cur.getColumnIndex(MediaStore.Audio.Playlists.Members.PLAY_ORDER));
        cur.close();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + 1);
        values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, parts[parts.length - 1]);
        resolver.insert(uri, values);
        Toast.makeText(context, "Added to " + playlist, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void gotoAlbum(MediaBrowserCompat.MediaItem item) {
        if (item.getDescription().getExtras() == null || !item.getDescription().getExtras().containsKey(Keys.EXTRA_ALBUM_ID))
            return;

        Intent intent = new Intent(context, ListExpandActivity.class);
        intent.putExtra(Keys.EXTRA_PARENT_ID, "ALBUMS/" + item.getDescription().getExtras().getString(Keys.EXTRA_ALBUM_ID));
        intent.putExtra(Keys.EXTRA_TYPE, "album");
        intent.putExtra(Keys.EXTRA_TITLE, item.getDescription().getDescription());
        context.startActivity(intent);
    }

    @Override
    public void gotoArtist(MediaBrowserCompat.MediaItem item) {
        if (item.getDescription().getExtras() == null || !item.getDescription().getExtras().containsKey(Keys.EXTRA_ARTIST_ID))
            return;
        Intent intent = new Intent(context, ListExpandActivity.class);
        intent.putExtra(Keys.EXTRA_PARENT_ID, "ARTISTS/" + item.getDescription().getExtras().getString(Keys.EXTRA_ARTIST_ID));
        intent.putExtra(Keys.EXTRA_TYPE, "artist");
        intent.putExtra(Keys.EXTRA_TITLE, item.getDescription().getSubtitle());
        context.startActivity(intent);
    }

    @Override
    public void shareSong(MediaBrowserCompat.MediaItem item) {
        String[] parts = item.getMediaId().split("[/|]");
        long mediaId = Long.parseLong(parts[parts.length - 1]);
        Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("audio/*");
        intent.putExtra(Intent.EXTRA_STREAM, contentUri);
        intent.putExtra(Intent.EXTRA_TITLE,item.getDescription().getTitle());
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share song via"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean deleteFromStorage(MediaBrowserCompat.MediaItem item, ActivityResultLauncher<IntentSenderRequest> launcher) {
        String[] parts = item.getMediaId().split("[/|]");
        long mediaId = Long.parseLong(parts[parts.length - 1]);
        Uri fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Cursor cursor = context.getContentResolver().query(fileUri, new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                @SuppressLint("Range") File file = new File(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                if (file.delete()) {
                    int x = context.getContentResolver().delete(fileUri, null, null);
                    LogHelper.d(TAG, "deleteFromStorage: " + fileUri.getEncodedPath() + " exist:" + file.exists());
                    Toast.makeText(context, "File Deleted", Toast.LENGTH_SHORT).show();
                    return x != 0;
                } else {
                    Toast.makeText(context, "File Not Deleted", Toast.LENGTH_SHORT).show();
                    return false;
                }

            }
            cursor.close();
        } else {
            StorageXI.getInstance().with(context).delete(launcher, fileUri);
        }

        return false;
    }
}
