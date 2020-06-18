package com.yash.ymplayer.util;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.core.content.FileProvider;
import androidx.core.graphics.PathUtils;
import androidx.recyclerview.widget.RecyclerView;

import com.yash.ymplayer.ListExpandActivity;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.ui.main.SongContextMenuListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class SongsContextMenuClickListener implements SongContextMenuListener {
    private static final String TAG = "debug";
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
        mMediaController.getTransportControls().sendCustomAction(Keys.Action.QUEUE_NEXT, extras);
    }

    @Override
    public void addToPlaylist(MediaBrowserCompat.MediaItem item, String playlist) {
        String[] parts = item.getDescription().getMediaId().split("[/|]");
        Log.d(TAG, "addToPlaylist: id:" + parts[parts.length - 1]);
        Bundle extras = new Bundle();
        extras.putString(Keys.MEDIA_ID, parts[parts.length - 1]);
        extras.putString(Keys.TITLE, item.getDescription().getTitle().toString());
        extras.putString(Keys.ARTIST, item.getDescription().getSubtitle().toString());
        extras.putString(Keys.ALBUM, item.getDescription().getDescription().toString());
        extras.putString(Keys.PLAYLIST_NAME, playlist);
        mMediaController.getTransportControls().sendCustomAction(Keys.Action.ADD_TO_PLAYLIST, extras);
    }

    @Override
    public void gotoAlbum(MediaBrowserCompat.MediaItem item) {
        Intent intent = new Intent(context, ListExpandActivity.class);
        intent.putExtra("parent_id", item.getDescription().getDescription());
        intent.putExtra("type", "album");
        intent.putExtra("imageId", Long.parseLong(Repository.getInstance(context).getOfflineProvider().getAlbumId(item.getMediaId())));
        context.startActivity(intent);
    }

    @Override
    public void gotoArtist(MediaBrowserCompat.MediaItem item) {
        Intent intent = new Intent(context, ListExpandActivity.class);
        intent.putExtra("parent_id", item.getDescription().getSubtitle());
        intent.putExtra("type", "artist");
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
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, "Share app via"));
    }

    @Override
    public boolean deleteFromStorage(MediaBrowserCompat.MediaItem item) {
        String[] parts = item.getMediaId().split("[/|]");
        long mediaId = Long.parseLong(parts[parts.length - 1]);
        Uri fileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Cursor cursor = context.getContentResolver().query(fileUri, new String[]{MediaStore.Audio.Media.DATA}, null, null, null);
            while (cursor != null && cursor.moveToNext()) {
                File file = new File(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
                if (file.delete()) {
                    int x = context.getContentResolver().delete(fileUri, null, null);
                    Log.d(TAG, "deleteFromStorage: " + fileUri.getEncodedPath() + " exist:" + file.exists());
                    Toast.makeText(context, "File Deleted", Toast.LENGTH_SHORT).show();
                    return x!=0;
                } else {
                    Toast.makeText(context, "File Not Deleted", Toast.LENGTH_SHORT).show();
                    return false;
                }

            }
            cursor.close();
        } else {
            context.revokeUriPermission(fileUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            int x = context.getContentResolver().delete(fileUri, null, null);
            Log.d(TAG, "deleteFromStorage: " + fileUri.getEncodedPath());
            return x != 0;
        }

        return false;
    }
}
