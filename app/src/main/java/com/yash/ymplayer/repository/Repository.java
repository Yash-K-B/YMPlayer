package com.yash.ymplayer.repository;

import android.content.Context;
import android.media.session.MediaSession;
import android.net.Uri;
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

import com.yash.ymplayer.storage.DeviceAudioProvider;
import com.yash.ymplayer.storage.MediaItem;
import com.yash.ymplayer.storage.MediaItemDao;
import com.yash.ymplayer.storage.OfflineMediaProvider;
import com.yash.ymplayer.storage.PlayList;
import com.yash.ymplayer.storage.PlayListObject;
import com.yash.ymplayer.storage.PlaylistMediaProvider;
import com.yash.ymplayer.ui.main.Playlists;
import com.yash.ymplayer.util.Keys;

import java.security.Key;
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
    DeviceAudioProvider audioProvider;

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
        RoomDatabase.Callback databaseCallback = new RoomDatabase.Callback() {
            @Override
            public void onCreate(@NonNull SupportSQLiteDatabase db) {
                db.execSQL("insert into playlist values('Favourites')");
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };
        provider = Room.databaseBuilder(context, PlaylistMediaProvider.class, "Playlists").allowMainThreadQueries().fallbackToDestructiveMigration().addCallback(databaseCallback).build();
        offlineProvider = new OfflineMediaProvider(context);
        audioProvider = new DeviceAudioProvider(context);
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

        if (playlist.equals("FAVOURITE")) {
            for (MediaItem item : mediaItemDao.getMediaItemsOfPlaylist(Keys.PLAYLISTS.FAVOURITES)) {
                MediaBrowserCompat.MediaItem song = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId + "|" + item.getMediaId())
                        .setTitle(item.getName())
                        .setIconUri(item.getArtwork() == null ? null : Uri.parse(item.getArtwork()))
                        .setSubtitle(item.getArtist())
                        .setDescription(item.getAlbum())
                        .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                songs.add(song);
            }
        } else if (playlist.equals("LAST_ADDED")) {
            return audioProvider.getLastAdded();
        } else {
            return audioProvider.getPlaylistSongs(mediaId);
        }


        return songs;
    }

//    public List<MediaBrowserCompat.MediaItem> getAllPlaylists() {
//
//
//        List<MediaBrowserCompat.MediaItem> playlists = new ArrayList<>();
//
//        for (PlayListObject playlist : mediaItemDao.getPlaylists()) {
//            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
//                    .setMediaId(playlist.getPlaylist())
//                    .setTitle(playlist.getPlaylist())
//                    //.setSubtitle(playlist.getCount() > 1 ? playlist.getCount() + " - songs" : playlist.getCount() + " - song")
//                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
//            playlists.add(item);
//            Log.d(TAG, "GetAllPlaylists : " + playlist.getPlaylist() + "    " + "PLAYLISTS/" + playlist.getPlaylist());
//        }
//
//
//        return playlists;
//
//    }

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
            switch (parts[1]) {
                case "FAVOURITE":
                    int i = 0;
                    for (MediaItem item : mediaItemDao.getMediaItemsOfPlaylist(Keys.PLAYLISTS.FAVOURITES)) {
                        MediaSessionCompat.QueueItem song = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                                .setMediaId(item.getMediaId())
                                .setTitle(item.getName())
                                .setSubtitle(item.getArtist())
                                .setIconUri(item.getArtwork() == null ? null : Uri.parse(item.getArtwork()))
                                .setDescription(item.getAlbum())
                                .build(), i);
                        items.add(song);
                    }
                    break;
                case "LAST_ADDED":
                    items.addAll(audioProvider.getLastAddedQueue());
                    break;
                case "RECENT":

                    break;
                default:
                    items.addAll(audioProvider.getPlaylistSongsQueue(mediaId));
                    break;
            }
        } else {
            Log.d(TAG, "getCurrentPlayingQueue: OfflineProvider");
            items.addAll(audioProvider.getPlayingQueue(mediaId));
        }
        return items;
    }

    public long isAddedTo(String mediaId, String playlist) {
        Log.d(TAG, "isAddedTo: " + mediaItemDao.isAddedTo(mediaId, playlist));
        return mediaItemDao.isAddedTo(mediaId, playlist);
    }

    public void removeFromPlaylist(String mediaId, String playlist) {
        mediaItemDao.removeFromPlaylist(mediaId, playlist);
    }

    public List<MediaSessionCompat.QueueItem> getRandomQueue() {
        return audioProvider.getRandomQueue();
    }

    public List<MediaSessionCompat.QueueItem> getQueue(int queueHint, String mediaId) {
        return audioProvider.getQueue(queueHint, mediaId);
    }

    public List<MediaBrowserCompat.MediaItem> getAllSongs() {
        return audioProvider.getAllSongs();
    }

    public List<MediaBrowserCompat.MediaItem> getAllAlbums() {
        return audioProvider.getAllAlbums();
    }

    public List<MediaBrowserCompat.MediaItem> getAllArtists() {
        return audioProvider.getAllArtists();
    }

    public List<MediaBrowserCompat.MediaItem> getAllPlaylists() {
        return audioProvider.getAllPlaylists();
    }

    public List<MediaBrowserCompat.MediaItem> getSongsOfAlbum(String albumId) {
        return audioProvider.getSongsOfAlbum(albumId);
    }

    public List<MediaBrowserCompat.MediaItem> getSongsOfArtist(String artistId) {
        return audioProvider.getSongsOfArtist(artistId);
    }


}
