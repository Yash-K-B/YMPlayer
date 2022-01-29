package com.yash.ymplayer.storage;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.media.MediaBrowserServiceCompat;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.util.Keys;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DeviceAudioProvider implements AudioProvider {
    private static final String TAG = "DeviceAudioProvider";
    ContentResolver resolver;
    private Cursor songsCursor;

    private static DeviceAudioProvider instance;

    public static DeviceAudioProvider getInstance(Context context) {
        if (instance == null)
            instance = new DeviceAudioProvider(context);
        return instance;
    }

    public DeviceAudioProvider(@NonNull Context context) {
        this.resolver = context.getApplicationContext().getContentResolver();
    }

    /**
     * @return list of mediaItem
     */
    @Override
    public List<MediaBrowserCompat.MediaItem> getAllSongs() {
        songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getSongsSelection(), null, getSongsSortOrder());
        List<MediaBrowserCompat.MediaItem> allSongs = new ArrayList<>();
        if (songsCursor == null) return allSongs;
        while (songsCursor.moveToNext()) {
            long mediaId = songsCursor.getLong(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            artist = (artist == null || artist.equalsIgnoreCase("<unknown>")) ? "Unknown Artist" : artist;
            String artistId = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
            String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            album = (album == null || album.equalsIgnoreCase("<unknown>")) ? "Unknown Album" : album;
            String albumId = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
            Bundle extras = new Bundle();
            extras.putString(Keys.EXTRA_ALBUM_ID, albumId);
            extras.putString(Keys.EXTRA_ARTIST_ID, artistId);
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId("ALL_SONGS|" + mediaId)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .setExtras(extras)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            allSongs.add(item);
        }
        songsCursor.close();
        return allSongs;
    }

    /**
     * @param albumId the id of the album
     * @return list of mediaItem
     */
    @Override
    public List<MediaBrowserCompat.MediaItem> getSongsOfAlbum(@NotNull String albumId) {
        String[] ids = albumId.split("[/]", 2);
        songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getAlbumSongsSelection(ids[1]), null, getSongsSortOrder());
        List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
        if (songsCursor == null) return songs;
        while (songsCursor.moveToNext()) {
            long mediaId = songsCursor.getLong(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            artist = (artist == null || artist.equalsIgnoreCase("<unknown>")) ? "Unknown Artist" : artist;
            String artistId = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
            String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            album = (album == null || album.equalsIgnoreCase("<unknown>")) ? "Unknown Album" : album;
            Bundle extras = new Bundle();
            extras.putString(Keys.EXTRA_ARTIST_ID, artistId);
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(albumId + "|" + mediaId)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .setExtras(extras)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            songs.add(item);
        }
        songsCursor.close();
        return songs;
    }

    /**
     * @param artistId id of the artist
     * @return list of mediaItem
     */
    @Override
    public List<MediaBrowserCompat.MediaItem> getSongsOfArtist(@NotNull String artistId) {
        String[] ids = artistId.split("[/]", 2);
        songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getArtistSongsSelection(ids[1]), null, getSongsSortOrder());
        List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();
        if (songsCursor == null) return songs;
        while (songsCursor.moveToNext()) {
            long mediaId = songsCursor.getLong(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            artist = (artist == null || artist.equalsIgnoreCase("<unknown>")) ? "Unknown Artist" : artist;
            String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            album = (album == null || album.equalsIgnoreCase("<unknown>")) ? "Unknown Album" : album;
            String albumId = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
            Bundle extras = new Bundle();
            extras.putString(Keys.EXTRA_ALBUM_ID, albumId);
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(artistId + "|" + mediaId)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .setExtras(extras)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            songs.add(item);
        }
        songsCursor.close();
        return songs;
    }


    private Uri getSongsUri() {
        return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }

    private String[] getSongsProjection() {
        return new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID
        };
    }

    private String getAlbumSongsSelection(@NonNull String albumId) {
        return MediaStore.Audio.Media.ALBUM_ID + " = " + albumId + " and " + MediaStore.Audio.Media.IS_MUSIC + " != 0";
    }

    private String getArtistSongsSelection(@NonNull String artistId) {
        return MediaStore.Audio.Media.ARTIST_ID + " = " + artistId + " and " + MediaStore.Audio.Media.IS_MUSIC + " != 0";
    }

    private String getSongSelection(@NonNull String mediaId) {
        return MediaStore.Audio.Media._ID + "=" + mediaId + " and " + MediaStore.Audio.Media.IS_MUSIC + " != 0";
    }

    private String getSongsSelection() {
        return MediaStore.Audio.Media.IS_MUSIC + " != 0";
    }

    private String getSongsSortOrder() {
        return MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";
    }

    /**
     * @return list of mediaItem
     */
    @Override
    public List<MediaBrowserCompat.MediaItem> getAllAlbums() {
        Cursor albumsCursor = resolver.query(getAlbumsUri(), getAlbumsProjection(), null, null, getAlbumsSortOrder());
        List<MediaBrowserCompat.MediaItem> allAlbums = new ArrayList<>();
        if (albumsCursor == null) return allAlbums;
        while (albumsCursor.moveToNext()) {
            long albumId = albumsCursor.getLong(albumsCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
            String album = albumsCursor.getString(albumsCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
            album = (album == null || album.equalsIgnoreCase("<unknown>")) ? "Unknown Album" : album;
            String artist = albumsCursor.getString(albumsCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
            int numberOfSongs = albumsCursor.getInt(albumsCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS));
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId("ALBUMS/" + albumId)
                    .setTitle(album)
                    .setSubtitle(numberOfSongs > 1 ? numberOfSongs + " - songs" : numberOfSongs + " - song")
                    .setDescription(artist)
                    .setIconUri(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId))
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
            allAlbums.add(item);
        }
        albumsCursor.close();
        Collections.sort(allAlbums, (o1, o2) -> o1.getDescription().getTitle().toString().compareToIgnoreCase(o2.getDescription().getTitle().toString()));
        return allAlbums;
    }


    private Uri getAlbumsUri() {
        return MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    }

    private String[] getAlbumsProjection() {
        return new String[]{
                MediaStore.Audio.Albums._ID,
                /*" REPLACE (" + MediaStore.Audio.Albums.ALBUM + ", '<unknown>' , 'Unknown Album') as " +*/ MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS,
        };
    }


    private String getAlbumsSortOrder() {
        return MediaStore.Audio.Albums.ALBUM + " COLLATE NOCASE ASC";
    }

    /**
     * @return list of mediaItem
     */
    @Override
    public List<MediaBrowserCompat.MediaItem> getAllArtists() {
        Cursor artistsCursor = resolver.query(getArtistsUri(), getArtistsProjection(), null, null, getArtistsSortOrder());
        List<MediaBrowserCompat.MediaItem> allArtists = new ArrayList<>();
        if (artistsCursor == null) return allArtists;
        while (artistsCursor.moveToNext()) {
            String artistId = artistsCursor.getString(artistsCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID));
            String artist = artistsCursor.getString(artistsCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST));
            artist = (artist == null || artist.equalsIgnoreCase("<unknown>")) ? "Unknown Artist" : artist;
            int numberOfTracks = artistsCursor.getInt(artistsCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS));
            int numberOfAlbums = artistsCursor.getInt(artistsCursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS));
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId("ARTISTS/" + artistId)
                    .setTitle(artist)
                    .setSubtitle(numberOfTracks > 1 ? numberOfTracks + " - songs" : numberOfTracks + " - song")
                    .setDescription(Integer.valueOf(numberOfAlbums).toString())
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
            allArtists.add(item);
        }
        artistsCursor.close();
        Collections.sort(allArtists, ((o1, o2) -> o1.getDescription().getTitle().toString().compareToIgnoreCase(o2.getDescription().getTitle().toString())));
        return allArtists;
    }


    private Uri getArtistsUri() {
        return MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    }

    private String[] getArtistsProjection() {
        return new String[]{
                MediaStore.Audio.Artists._ID,
                /*" REPLACE (" + MediaStore.Audio.Artists.ARTIST + ", '<unknown>' , 'Unknown Artist') as " +*/ MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
                MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,

        };
    }


    private String getArtistsSortOrder() {
        return MediaStore.Audio.Artists.ARTIST + " COLLATE NOCASE ASC";
    }

    /**
     * @return list of mediaItem
     */
    @Override
    public List<MediaBrowserCompat.MediaItem> getAllPlaylists() {
        Cursor playlistCursor = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME, MediaStore.Audio.PlaylistsColumns.DATE_ADDED}, null, null, null);
        List<MediaBrowserCompat.MediaItem> allPlaylists = new ArrayList<>();
        if (playlistCursor == null) return allPlaylists;
        while (playlistCursor.moveToNext()) {
            String playlistId = playlistCursor.getString(playlistCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
            String playlist = playlistCursor.getString(playlistCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME));
            String addedDate = playlistCursor.getString(playlistCursor.getColumnIndexOrThrow(MediaStore.Audio.PlaylistsColumns.DATE_ADDED));
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId("PLAYLISTS/" + playlistId)
                    .setTitle(playlist)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
            allPlaylists.add(item);
        }
        playlistCursor.close();
        return allPlaylists;
    }

    @Override
    public List<MediaBrowserCompat.MediaItem> getPlaylistSongs(String mediaId) {
        String[] splits = mediaId.split("[/|]");
        long playlistId = Long.parseLong(splits[splits.length - 1]);
        List<MediaBrowserCompat.MediaItem> playlistSongs = new ArrayList<>();
        Cursor playlistSongsCursor = resolver.query(MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId), getPlaylistSongsProjection(), MediaStore.Audio.Media.IS_MUSIC + "!=0", null, null);
        if (playlistSongsCursor == null) return playlistSongs;
        while (playlistSongsCursor.moveToNext()) {
            String id = playlistSongsCursor.getString(playlistSongsCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID));
            String title = playlistSongsCursor.getString(playlistSongsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = playlistSongsCursor.getString(playlistSongsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String artistId = playlistSongsCursor.getString(playlistSongsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
            String album = playlistSongsCursor.getString(playlistSongsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            String albumId = playlistSongsCursor.getString(playlistSongsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
            Bundle extras = new Bundle();
            extras.putString(Keys.EXTRA_ARTIST_ID, artistId);
            extras.putString(Keys.EXTRA_ALBUM_ID, albumId);
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId("PLAYLISTS/" + playlistId + "|" + id)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .setExtras(extras)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            playlistSongs.add(item);
        }
        playlistSongsCursor.close();
        return playlistSongs;
    }

    public List<MediaBrowserCompat.MediaItem> getLastAdded() {
        Cursor lastAddedCursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.ARTIST_ID}, MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.DATE_ADDED + " DESC");
        List<MediaBrowserCompat.MediaItem> lastAddedSongs = new ArrayList<>();
        if (lastAddedCursor == null) return lastAddedSongs;
        while (lastAddedCursor.moveToNext()) {
            String mediaId = lastAddedCursor.getString(lastAddedCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            String title = lastAddedCursor.getString(lastAddedCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = lastAddedCursor.getString(lastAddedCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String artistId = lastAddedCursor.getString(lastAddedCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID));
            String album = lastAddedCursor.getString(lastAddedCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            String albumId = lastAddedCursor.getString(lastAddedCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
            Bundle extras = new Bundle();
            extras.putString(Keys.EXTRA_ARTIST_ID, artistId);
            extras.putString(Keys.EXTRA_ALBUM_ID, albumId);
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId("PLAYLISTS/LAST_ADDED|" + mediaId)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .setExtras(extras)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            lastAddedSongs.add(item);
        }
        lastAddedCursor.close();
        return lastAddedSongs;
    }

    /**
     * @param mediaId mediaId of the song
     * @return list of QueueItem
     */
    @Override
    public List<MediaSessionCompat.QueueItem> getPlayingQueue(String mediaId) {
        String[] parts = mediaId.split("[/|]");
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        if (parts[0] == null) return items;
        long queueId = 0;
        switch (parts[0]) {
            case Keys.QueueType.ALL_SONGS:
                songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getSongsSelection(), null, getSongsSortOrder());
                if (songsCursor == null) return items;
                while (songsCursor.moveToNext()) {
                    String id = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                            .setMediaId(id)
                            .setTitle(title)
                            .setSubtitle(artist)
                            .setDescription(album)
                            .build(), queueId++);
                    items.add(item);
                }
                songsCursor.close();
                break;
            case Keys.QueueType.ARTISTS:
                String artistId = parts[1];
                songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getArtistSongsSelection(artistId), null, getSongsSortOrder());
                if (songsCursor == null) return items;
                while (songsCursor.moveToNext()) {
                    String id = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                            .setMediaId(id)
                            .setTitle(title)
                            .setSubtitle(artist)
                            .setDescription(album)
                            .build(), queueId++);
                    items.add(item);
                }
                songsCursor.close();
                break;
            case Keys.QueueType.ALBUMS:
                String albumId = parts[1];
                songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getAlbumSongsSelection(albumId), null, getSongsSortOrder());
                if (songsCursor == null) return items;
                while (songsCursor.moveToNext()) {
                    String id = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                    String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                            .setMediaId(id)
                            .setTitle(title)
                            .setSubtitle(artist)
                            .setDescription(album)
                            .build(), queueId++);
                    items.add(item);
                }
                songsCursor.close();
                break;
        }
        return items;
    }

    /**
     * @return list of QueueItem
     */
    @Override
    public List<MediaSessionCompat.QueueItem> getRandomQueue() {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getSongsSelection(), null, " RANDOM() LIMIT 15");
        if (songsCursor == null) return items;
        long queueId = 0;
        while (songsCursor.moveToNext()) {
            String id = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(id)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .build(), queueId++);
            items.add(item);
        }
        songsCursor.close();
        return items;
    }

    /**
     * @param queueHint
     * @param mediaId
     * @return list of QueueItem
     */
    @Override
    public List<MediaSessionCompat.QueueItem> getQueue(int queueHint, String mediaId) {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        String[] splits = mediaId.split("[/|]");
        String id = splits[splits.length - 1];

        switch (queueHint) {
            case QueueHint.SINGLE_SONG:
                songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getSongSelection(id), null, getSongsSortOrder());
                break;
            case QueueHint.ALBUM_SONGS:
                songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getAlbumSongsSelection(id), null, getSongsSortOrder());
                break;
            case QueueHint.ARTIST_SONGS:
                songsCursor = resolver.query(getSongsUri(), getSongsProjection(), getArtistSongsSelection(id), null, getSongsSortOrder());
                break;
            case QueueHint.PLAYLIST_SONGS:
            default:
                return items;
        }

        if (songsCursor == null) return items;
        long queueId = 0;
        while (songsCursor.moveToNext()) {
            String mediaID = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(mediaID)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .build(), queueId++);
            items.add(item);
        }
        songsCursor.close();
        return items;
    }

    @Override
    public List<MediaSessionCompat.QueueItem> getLastAddedQueue() {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        songsCursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM}, MediaStore.Audio.Media.IS_MUSIC + "!=0", null, MediaStore.Audio.Media.DATE_ADDED + " DESC");
        if (songsCursor == null) return items;
        long queueId = 0;
        while (songsCursor.moveToNext()) {
            String id = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(id)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .build(), queueId++);
            items.add(item);
        }
        songsCursor.close();
        return items;
    }

    @Override
    public List<MediaSessionCompat.QueueItem> getPlaylistSongsQueue(String mediaId) {
        String[] splits = mediaId.split("[/|]", 3);
        long playlistId = Long.parseLong(splits[1]);
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        songsCursor = resolver.query(MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId), getPlaylistSongsProjection(), MediaStore.Audio.Media.IS_MUSIC + "!=0", null, null);
        if (songsCursor == null) return items;
        long queueId = 0;
        while (songsCursor.moveToNext()) {
            String id = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID));
            String title = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String album = songsCursor.getString(songsCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
            MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(id)
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setDescription(album)
                    .build(), queueId++);
            items.add(item);
        }
        songsCursor.close();
        return items;
    }

    String[] getPlaylistSongsProjection() {
        return new String[]{
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ALBUM_ID

        };
    }


}
