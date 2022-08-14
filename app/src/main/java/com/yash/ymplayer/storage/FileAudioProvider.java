package com.yash.ymplayer.storage;

import android.content.ContentResolver;
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;

import com.yash.logging.LogHelper;
import com.yash.logging.utils.ExceptionUtil;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileAudioProvider {

    private static final String TAG = "FileAudioProvider";

    private ContentResolver resolver;
    private Context context;

    private static DeviceAudioProvider instance;

    public static DeviceAudioProvider getInstance(Context context) {
        if (instance == null)
            instance = new DeviceAudioProvider(context);
        return instance;
    }

    public FileAudioProvider(@NonNull Context context) {
        this.resolver = context.getApplicationContext().getContentResolver();
        this.context = context;
    }


    public List<MediaSessionCompat.QueueItem> getPlayingQueue(Uri uri) {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        int queueId = 0;

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(context, uri);

        String songName = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String album = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        String genre = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        String track = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS);

        LogHelper.d(TAG, "SongName: " + songName + " Artist: " + artist + " Album: " + album);
        LogHelper.d(TAG, "Genre: " + genre + " Track: " + track);

        MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                .setMediaId(uri.toString())
                .setTitle(songName)
                .setSubtitle(artist)
                .setDescription(album)
                .build(), queueId);
        items.add(item);

        return items;
    }
}
