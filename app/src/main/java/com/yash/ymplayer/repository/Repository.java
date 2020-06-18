package com.yash.ymplayer.repository;

import android.content.Context;
import android.media.session.MediaSession;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.yash.ymplayer.storage.MediaItem;
import com.yash.ymplayer.storage.MediaItemDao;
import com.yash.ymplayer.storage.OfflineMediaProvider;
import com.yash.ymplayer.storage.PlayList;
import com.yash.ymplayer.storage.PlayListObject;
import com.yash.ymplayer.storage.PlaylistMediaProvider;
import com.yash.ymplayer.ui.main.Playlists;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Repository {
    private static final String TAG = "debug";
    private PlaylistMediaProvider provider;
    OfflineMediaProvider offlineProvider;
    MediaItemDao mediaItemDao;
    static Repository instance;
    Context context;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    public static Repository getInstance(Context context) {
        if (instance == null)
            instance = new Repository(context);
        return instance;
    }

    public static Repository getRefreshedInstance(Context context) {
        instance = new Repository(context);
        return instance;
    }

    Repository(Context context) {
        provider = Room.databaseBuilder(context, PlaylistMediaProvider.class, "Playlists").allowMainThreadQueries().fallbackToDestructiveMigration().addCallback(DatabaseCallback).build();
        offlineProvider = new OfflineMediaProvider(context);
        mediaItemDao = provider.getMediaItemDao();
        this.context = context;
        Log.d(TAG, "Repository: New Instance");
    }

    public OfflineMediaProvider getOfflineProvider() {
        return offlineProvider;
    }


    public List<MediaBrowserCompat.MediaItem> getAllSongsOfPlaylist(String mediaId) {
        String[] parts = mediaId.split("[/|]");
        String playlist = parts[parts.length - 1];
        List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();

        for (MediaItem item : mediaItemDao.getMediaItemsOfPlaylist(playlist)) {
            MediaBrowserCompat.MediaItem song = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId + "|" + item.getMediaId())
                    .setTitle(item.getName())
                    .setSubtitle(item.getArtist())
                    .setDescription(item.getAlbum())
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            songs.add(song);
        }


        return songs;
    }

    public List<MediaBrowserCompat.MediaItem> getAllPlaylists() {
        List<MediaBrowserCompat.MediaItem> playlists = new ArrayList<>();

        for (PlayListObject playlist : mediaItemDao.getPlaylists()) {
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(playlist.getPlaylist())
                    .setTitle(playlist.getPlaylist())
                    //.setSubtitle(playlist.getCount() > 1 ? playlist.getCount() + " - songs" : playlist.getCount() + " - song")
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
            playlists.add(item);
            Log.d(TAG, "GetAllPlaylists : " + playlist.getPlaylist() + "    " + "PLAYLISTS/" + playlist.getPlaylist());
        }


        return playlists;

    }

    public long addToPlaylist(MediaItem item) {
        return mediaItemDao.insert(item);
    }

    public long createPlaylist(String playlist) {
        return mediaItemDao.insert(new PlayList(playlist));
    }

    public List<MediaSessionCompat.QueueItem> getCurrentPlayingQueue(String mediaId) {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        if (mediaId.contains("PLAYLISTS")) {
            String[] parts = mediaId.split("[/|]", 3);
            int i = 0;
            for (MediaItem item : mediaItemDao.getMediaItemsOfPlaylist(parts[1])) {
                MediaSessionCompat.QueueItem song = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(item.getMediaId())
                        .setTitle(item.getName())
                        .setSubtitle(item.getArtist())
                        .setDescription(item.getAlbum())
                        .build(), Long.parseLong(item.getMediaId()));
                items.add(song);
            }
        } else {
            Log.d(TAG, "getCurrentPlayingQueue: OfflineProvider");
            items.addAll(offlineProvider.getCurrentPlayingQueue(mediaId));
        }
        return items;
    }

    private final RoomDatabase.Callback DatabaseCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("insert into playlist values('Favourites')");
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
        }
    };

    public long isAddedTo(String mediaId, String playlist) {
        Log.d(TAG, "isAddedTo: " + mediaItemDao.isAddedTo(mediaId, playlist));
        return mediaItemDao.isAddedTo(mediaId, playlist);
    }

    public void removeFromPlaylist(String mediaId, String playlist) {
        mediaItemDao.removeFromPlaylist(mediaId, playlist);
    }
}
