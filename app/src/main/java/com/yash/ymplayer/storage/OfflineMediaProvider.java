package com.yash.ymplayer.storage;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadata;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.yash.logging.LogHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class OfflineMediaProvider {
    public static final String METADATA_KEY_ALBUM_ID = "albumId";
    private static final String TAG = "debug";
    public static final String METADATA_KEY_FIRST_ITEM_ID = "first_item_media_id";
    private List<MediaMetadata> songs;
    private List<MediaMetadataCompat> playlists;
    private HashMap<String, MediaMetadata> songById;
    private HashMap<String, List<MediaMetadata>> songsByArtist;
    private HashMap<String, List<MediaMetadata>> songsByAlbum;
    private boolean isMusicFetched;
    ContentResolver resolver;
    private Cursor cursor;
    private Cursor albumCursor;
    private Cursor artistCursor;
    private Cursor playlistCursor;
    private boolean isPlaylistFetched;


    private static OfflineMediaProvider instance;

    public static OfflineMediaProvider getInstance(Context context) {
        if (instance == null)
            instance = new OfflineMediaProvider(context.getApplicationContext());
        return instance;
    }

    public OfflineMediaProvider(Context context) {
        songs = new ArrayList<>();
        playlists = new ArrayList<>();
        songById = new HashMap<>();
        songsByArtist = new HashMap<>();
        songsByAlbum = new HashMap<>();
        isMusicFetched = false;
        isPlaylistFetched = false;
        resolver = context.getApplicationContext().getContentResolver();
        cursor = resolver.query(getUri(), getProjection(), getSelectionArg(), null, getSortOrder());
       // playlistCursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Playlists._ID,MediaStore.Audio.Playlists.NAME,MediaStore.Audio.Playlists._COUNT,MediaStore.Audio.Playlists.CONTENT_TYPE}, null, null, null);
    }


    /**
     * The method initialises the music
     */
    private void fetchMusic() {
        while (cursor.moveToNext()) {
            String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
            @SuppressLint("WrongConstant") MediaMetadata item = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)))
                    .putString(MediaMetadata.METADATA_KEY_TITLE, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)))
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist.equalsIgnoreCase("<unknown>") ? "Unknown Artist" : artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, album.equalsIgnoreCase("<unknown>") ? "Unknown Album" : album)
                    .putString(OfflineMediaProvider.METADATA_KEY_ALBUM_ID, cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)))
                    .build();
            songs.add(item);
            songById.put(item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID), item);
            // Log.d(TAG, "fetchMusic: id:"+item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)+" object:"+songById.get(item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)));
            List<MediaMetadata> songsOfArtist = songsByArtist.get(item.getString(MediaMetadata.METADATA_KEY_ARTIST));
            if (songsOfArtist == null)
                songsOfArtist = new ArrayList<>();
            songsOfArtist.add(item);
            songsByArtist.put(item.getString(MediaMetadata.METADATA_KEY_ARTIST), songsOfArtist);

            List<MediaMetadata> songsOfAlbum = songsByAlbum.get(item.getString(MediaMetadata.METADATA_KEY_ALBUM));
            if (songsOfAlbum == null)
                songsOfAlbum = new ArrayList<>();
            songsOfAlbum.add(item);
            songsByAlbum.put(item.getString(MediaMetadata.METADATA_KEY_ALBUM), songsOfAlbum);
        }
        isMusicFetched = true;
    }

    /**
     * @return list of all songs
     */
    public List<MediaItem> getAllSongs() {
        List<MediaItem> items = new ArrayList<>();
        if (!isMusicFetched) {
            fetchMusic();
        }
        for (MediaMetadata item : songs) {
            MediaItem child = new MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId("ALL_SONGS|" + item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
                    .setTitle(item.getString(MediaMetadata.METADATA_KEY_TITLE))
                    .setSubtitle(item.getString(MediaMetadata.METADATA_KEY_ARTIST))
                    .setDescription(item.getString(MediaMetadata.METADATA_KEY_ALBUM))
                    .build(), MediaItem.FLAG_PLAYABLE);
            items.add(child);
        }
        return items;
    }

    public List<MediaSessionCompat.QueueItem> getSongById(String mediaId, long queueId) {
        List<MediaSessionCompat.QueueItem> list = new ArrayList<>();
        if (!isMusicFetched) {
            fetchMusic();
        }
        String[] parts = mediaId.split("[/|]");
        String id = parts[parts.length - 1];
        //Log.d(TAG, "getSongById: "+id+"object :"+songById.get(id));
        try {
            Long.parseLong(id);
            list.add(new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(id)
                    .setTitle(songById.get(id).getString(MediaMetadata.METADATA_KEY_TITLE))
                    .setSubtitle(songById.get(id).getString(MediaMetadata.METADATA_KEY_ARTIST))
                    .setDescription(songById.get(id).getString(MediaMetadata.METADATA_KEY_ALBUM))
                    .build(), queueId));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }


    public List<MediaSessionCompat.QueueItem> getSongsById(String mediaId, long queueId, int type) {
        switch (type) {
            case QueueType.SONG:
                return getSongById(mediaId, queueId);
            case QueueType.ALBUM_SONGS:
            case QueueType.ARTIST_SONGS:
                return getQueue(mediaId);
            default:
                return new ArrayList<>();

        }
    }

    /**
     * List of all Artists
     *
     * @return list
     */
    public List<MediaItem> getAllArtists() {
        List<MediaItem> items = new ArrayList<>();
        if (!isMusicFetched) {
            fetchMusic();
        }
        List<String> keys = new ArrayList<>(songsByArtist.keySet());
        Collections.sort(keys, String::compareToIgnoreCase);
        for (String item : keys) {
            Bundle extras = new Bundle();
            extras.putString(METADATA_KEY_FIRST_ITEM_ID, songsByArtist.get(item).get(0).getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
            MediaItem child = new MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(item)
                    .setTitle(item)
                    .setSubtitle(songsByArtist.get(item).size() + " - " + ((songsByArtist.get(item).size() > 1) ? "songs" : "song"))
                    .setExtras(extras)
                    .build(), MediaItem.FLAG_BROWSABLE);
            items.add(child);
        }
        return items;
    }

    /**
     * @param parentId
     * @return
     */
    public List<MediaItem> getSongsOfArtist(String parentId) {
        String[] parts = parentId.split("/", 2);
        String artist = parts[parts.length - 1];
        List<MediaItem> items = new ArrayList<>();
        if (!isMusicFetched) {
            fetchMusic();
        }
        for (MediaMetadata item : songsByArtist.get(artist)) {
            MediaItem child = new MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(parentId + "|" + item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
                    .setTitle(item.getString(MediaMetadata.METADATA_KEY_TITLE))
                    .setSubtitle(item.getString(MediaMetadata.METADATA_KEY_ARTIST))
                    .setDescription(item.getString(MediaMetadata.METADATA_KEY_ALBUM))
                    .build(), MediaItem.FLAG_PLAYABLE);
            items.add(child);
        }
        return items;
    }

    @SuppressLint("WrongConstant")
    public List<MediaItem> getAllAlbums() {
        List<MediaItem> items = new ArrayList<>();
        if (!isMusicFetched) {
            fetchMusic();
        }
        List<String> keys = new ArrayList<>(songsByAlbum.keySet());
        Collections.sort(keys, String::compareToIgnoreCase);
        for (String item : keys) {
            Bundle extra = new Bundle();
            extra.putString(OfflineMediaProvider.METADATA_KEY_ALBUM_ID, songsByAlbum.get(item).get(0).getString(OfflineMediaProvider.METADATA_KEY_ALBUM_ID));
            extra.putString(METADATA_KEY_FIRST_ITEM_ID, songsByAlbum.get(item).get(0).getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
            MediaItem child = new MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(item)
                    .setTitle(item)
                    .setSubtitle(songsByAlbum.get(item).size() + " - " + ((songsByAlbum.get(item).size() > 1) ? "songs" : "song"))
                    .setExtras(extra)
                    .build(), MediaItem.FLAG_BROWSABLE);
            items.add(child);
        }
        return items;
    }

    public List<MediaItem> getSongsOfAlbum(String parentId) {
        String[] parts = parentId.split("/", 2);
        String album = parts[parts.length - 1];
        List<MediaItem> items = new ArrayList<>();
        if (!isMusicFetched) {
            fetchMusic();
        }
        for (MediaMetadata item : songsByAlbum.get(album)) {
            MediaItem child = new MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(parentId + "|" + item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
                    .setTitle(item.getString(MediaMetadata.METADATA_KEY_TITLE))
                    .setSubtitle(item.getString(MediaMetadata.METADATA_KEY_ARTIST))
                    .setDescription(item.getString(MediaMetadata.METADATA_KEY_ALBUM))
                    .build(), MediaItem.FLAG_PLAYABLE);
            items.add(child);
        }
        return items;
    }

    public List<MediaSessionCompat.QueueItem> getCurrentPlayingQueue(String mediaId) {
        return getQueue(mediaId);
    }

    public List<MediaSessionCompat.QueueItem> getQueue(String mediaId) {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        String[] parts = mediaId.split("[/|]", 3);
        if (!isMusicFetched) {
            fetchMusic();
        }

        if (parts[0].equalsIgnoreCase("ALL_SONGS")) {
            for (int i = 0; i < songs.size(); i++) {
                items.add(new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(songs.get(i).getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
                        .setTitle(songs.get(i).getString(MediaMetadata.METADATA_KEY_TITLE))
                        .setSubtitle(songs.get(i).getString(MediaMetadata.METADATA_KEY_ARTIST))
                        .setDescription(songs.get(i).getString(MediaMetadata.METADATA_KEY_ALBUM))
                        .build(), Long.parseLong(songs.get(i).getString(MediaMetadata.METADATA_KEY_MEDIA_ID))));
            }
        } else if (parts[0].equalsIgnoreCase("ARTISTS")) {
            for (MediaMetadata item : songsByArtist.get(parts[1])) {
                MediaSessionCompat.QueueItem child = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
                        .setTitle(item.getString(MediaMetadata.METADATA_KEY_TITLE))
                        .setSubtitle(item.getString(MediaMetadata.METADATA_KEY_ARTIST))
                        .setDescription(item.getString(MediaMetadata.METADATA_KEY_ALBUM))
                        .build(), Long.parseLong(item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)));
                items.add(child);
            }

        } else if (parts[0].equalsIgnoreCase("ALBUMS")) {
            for (MediaMetadata item : songsByAlbum.get(parts[1])) {
                MediaSessionCompat.QueueItem child = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                        .setMediaId(item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
                        .setTitle(item.getString(MediaMetadata.METADATA_KEY_TITLE))
                        .setSubtitle(item.getString(MediaMetadata.METADATA_KEY_ARTIST))
                        .setDescription(item.getString(MediaMetadata.METADATA_KEY_ALBUM))
                        .build(), Long.parseLong(item.getString(MediaMetadata.METADATA_KEY_MEDIA_ID)));
                items.add(child);
            }
        }

        return items;
    }

    /**
     * @return the Uri of MediaStorage
     */
    private Uri getUri() {
        return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }

    /**
     * The columns which are selected from audio table
     *
     * @return column names
     */
    private String[] getProjection() {
        return new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };
    }

    private String getSelectionArg() {
        return MediaStore.Audio.Media.IS_MUSIC + " != 0";
    }

    /**
     * Sort the list
     *
     * @return a sorting sql parameter
     */
    private String getSortOrder() {
        return MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";
    }

    public String getAlbumId(String mediaId) {
        String[] parts = mediaId.split("[/|]");
        String id = parts[parts.length - 1];
        Cursor c = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media.ALBUM_ID}, MediaStore.Audio.Media._ID + "=" + id, null, null);
        if (c.moveToNext()) {
            String albumId = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
            c.close();
            Log.d(TAG, "getAlbumId: " + albumId);
            return albumId;
        }
        c.close();
        return null;
    }

    public long getLongAlbumId(String mediaId) {
        return Long.parseLong(getAlbumId(mediaId));
    }

    public List<MediaSessionCompat.QueueItem> getRandomQueue() {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        if (!isMusicFetched) {
            fetchMusic();
        }
        List<MediaMetadata> list = new ArrayList<>(songs);
        LogHelper.d(TAG, "getRandomQueue: before:" + list.size());
        Collections.shuffle(list);
        LogHelper.d(TAG, "getRandomQueue: after:" + list.size());
        int size = Math.min(list.size(), 10);
        for (int i = 0; i < size; i++) {
            items.add(new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(list.get(i).getString(MediaMetadata.METADATA_KEY_MEDIA_ID))
                    .setTitle(list.get(i).getString(MediaMetadata.METADATA_KEY_TITLE))
                    .setSubtitle(list.get(i).getString(MediaMetadata.METADATA_KEY_ARTIST))
                    .setDescription(list.get(i).getString(MediaMetadata.METADATA_KEY_ALBUM))
                    .build(), Long.parseLong(list.get(i).getString(MediaMetadata.METADATA_KEY_MEDIA_ID))));
        }
        return items;
    }

//    synchronized void fetchPlaylist(){
//        MediaMetadataCompat item;
//        while (playlistCursor.moveToNext()){
//            String id = playlistCursor.getString(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists._ID));
//            String name = playlistCursor.getString(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME));
//            String count = playlistCursor.getString(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists._COUNT));
//            String contentType = playlistCursor.getString(playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.CONTENT_TYPE));
//            item = new MediaMetadataCompat.Builder()
//                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,id)
//                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE,name)
//                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,Integer.parseInt(count)>1?count+"-songs":count+"-song")
//                    .build();
//            playlists.add(item);
//        }
//        isPlaylistFetched = true;
//    }






    public interface QueueType {
        int ALBUM_SONGS = 0;
        int ARTIST_SONGS = 1;
        int SONG = 2;
    }
}
