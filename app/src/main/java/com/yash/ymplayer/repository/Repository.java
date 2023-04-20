package com.yash.ymplayer.repository;

import android.content.Context;
import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.yash.ymplayer.storage.DeviceAudioProvider;
import com.yash.ymplayer.storage.FileAudioProvider;
import com.yash.ymplayer.storage.MediaItem;
import com.yash.ymplayer.storage.dao.MediaItemDao;
import com.yash.ymplayer.storage.OfflineMediaProvider;
import com.yash.ymplayer.storage.PlayList;
import com.yash.ymplayer.storage.PlayListObject;
import com.yash.ymplayer.storage.PlaylistMediaProvider;
import com.yash.ymplayer.interfaces.Keys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Repository {
    private static final String TAG = "debug";
    private PlaylistMediaProvider provider;
    OfflineMediaProvider offlineProvider;
    MediaItemDao mediaItemDao;
    static Repository instance;
    Context context;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    DeviceAudioProvider audioProvider;
    FileAudioProvider fileAudioProvider;

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
                db.execSQL("insert into playlist (name) values('" + Keys.PLAYLISTS.FAVOURITES + "')");
                db.execSQL("insert into playlist (name) values('" + Keys.PLAYLISTS.LAST_PLAYED + "')");
            }

            @Override
            public void onOpen(@NonNull SupportSQLiteDatabase db) {
                super.onOpen(db);
            }
        };
        provider = Room.databaseBuilder(context, PlaylistMediaProvider.class, "Playlists.db").allowMainThreadQueries().fallbackToDestructiveMigration().addCallback(databaseCallback).build();
        offlineProvider = new OfflineMediaProvider(context);
        audioProvider = new DeviceAudioProvider(context);
        fileAudioProvider = new FileAudioProvider(context);
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

        if(parts[0].equals(Keys.PlaylistType.HYBRID_PLAYLIST.name())) {
            for (MediaItem item : mediaItemDao.getMediaItemsOfPlaylist(Integer.parseInt(playlist))) {
                MediaBrowserCompat.MediaItem song = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId + "|" + item.getMediaId())
                        .setTitle(item.getName())
                        .setIconUri(item.getArtwork() == null ? null : Uri.parse(item.getArtwork()))
                        .setSubtitle(item.getArtist())
                        .setDescription(item.getAlbum())
                        .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                songs.add(song);
            }
        } else if (playlist.equals("FAVOURITE")) {
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
        } else if (playlist.equals("LAST_PLAYED")) {
            for (MediaItem item : mediaItemDao.getMediaItemsOfPlaylistDesc(Keys.PLAYLISTS.LAST_PLAYED)) {
                MediaBrowserCompat.MediaItem song = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(mediaId + "|" + item.getMediaId())
                        .setTitle(item.getName())
                        .setIconUri(item.getArtwork() == null ? null : Uri.parse(item.getArtwork()))
                        .setSubtitle(item.getArtist())
                        .setDescription(item.getAlbum())
                        .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                songs.add(song);
            }
        } else {
            return audioProvider.getPlaylistSongs(mediaId);
        }


        return songs;
    }

    public List<MediaBrowserCompat.MediaItem> getHybridPlayLists() {

        Set<String> exclude = new HashSet<>(Arrays.asList(Keys.PLAYLISTS.FAVOURITES, Keys.PLAYLISTS.LAST_PLAYED));
        List<MediaBrowserCompat.MediaItem> playlists = new ArrayList<>();
        for (PlayListObject playlist : mediaItemDao.getPlaylists()) {
            if(exclude.contains(playlist.getName()))
                continue;
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(Keys.PlaylistType.HYBRID_PLAYLIST.name() + "/" + playlist.getId())
                    .setTitle(playlist.getName())
                    .setDescription(Keys.PlaylistType.HYBRID_PLAYLIST.name())
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
            playlists.add(item);
        }


        return playlists;

    }

    public long addToPlaylist(MediaItem item) {
        return mediaItemDao.insert(item);
    }

    public PlayList getPlaylist(String playlist) {
        return mediaItemDao.findPlaylist(playlist);
    }

    public void addLastPlayed(MediaItem item) {
        mediaItemDao.replace(item);
    }

    public long createPlaylist(String playlist) {
        return mediaItemDao.insert(new PlayList(playlist));
    }

    public void deletePlaylist(Integer id){
        mediaItemDao.deleteSongs(id);
        mediaItemDao.deletePlaylist(id);
    }

    public List<MediaSessionCompat.QueueItem> getCurrentPlayingQueue(String mediaId) {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        if(mediaId.startsWith(Keys.PlaylistType.HYBRID_PLAYLIST.name())) {
            String[] parts = mediaId.split("[/|]", 3);
            int i = 0;
            for (MediaItem item : mediaItemDao.getMediaItemsOfPlaylist(Integer.parseInt(parts[1]))) {
                MediaSessionCompat.QueueItem song = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(item.getMediaId())
                        .setTitle(item.getName())
                        .setSubtitle(item.getArtist())
                        .setIconUri(item.getArtwork() == null ? null : Uri.parse(item.getArtwork()))
                        .setDescription(item.getAlbum())
                        .build(), i++);
                items.add(song);
            }
        } else if (mediaId.contains("PLAYLISTS")) {
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
                                .build(), i++);
                        items.add(song);
                    }
                    break;
                case "LAST_ADDED":
                    items.addAll(audioProvider.getLastAddedQueue());
                    break;
                case "LAST_PLAYED":
                    i = 0;
                    for (MediaItem item : mediaItemDao.getMediaItemsOfPlaylistDesc(Keys.PLAYLISTS.LAST_PLAYED)) {
                        MediaSessionCompat.QueueItem song = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                                .setMediaId(item.getMediaId())
                                .setTitle(item.getName())
                                .setIconUri(item.getArtwork() == null ? null : Uri.parse(item.getArtwork()))
                                .setSubtitle(item.getArtist())
                                .setDescription(item.getAlbum())
                                .build(), i++);
                        items.add(song);
                    }
                    break;
                default:
                    items.addAll(audioProvider.getPlaylistSongsQueue(mediaId));
                    break;
            }
        } else if(mediaId.contains("URI")) {
            String[] parts = mediaId.split("[|]", 2);
            items.addAll(fileAudioProvider.getPlayingQueue(Uri.parse(parts[1])));

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
        List<MediaBrowserCompat.MediaItem> offlinePlaylists = audioProvider.getAllPlaylists();
        offlinePlaylists.addAll(getHybridPlayLists());
        return offlinePlaylists;
    }

    public List<MediaBrowserCompat.MediaItem> getSongsOfAlbum(String albumId) {
        return audioProvider.getSongsOfAlbum(albumId);
    }

    public List<MediaBrowserCompat.MediaItem> getSongsOfArtist(String artistId) {
        return audioProvider.getSongsOfArtist(artistId);
    }


    public void renamePlaylist(Integer id, String newName) {
        mediaItemDao.renamePlayList(id, newName);
    }
}
