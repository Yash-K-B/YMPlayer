package com.yash.ymplayer.storage;

import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.List;

public interface AudioProvider {
    /**
     * This method Queries the songs available in the device and map it to MediaItems
     *
     * @return list of MediaItems
     */
    List<MediaBrowserCompat.MediaItem> getAllSongs();

    /**
     * @param albumId the id of the album
     * @return list of MediaItems
     */
    List<MediaBrowserCompat.MediaItem> getSongsOfAlbum(String albumId);

    /**
     * @param artistId id of the artist
     * @return list of MediaItems
     */
    List<MediaBrowserCompat.MediaItem> getSongsOfArtist(String artistId);

    /**
     * @return list of MediaItems
     */
    List<MediaBrowserCompat.MediaItem> getAllAlbums();

    /**
     * @return list of MediaItems
     */
    List<MediaBrowserCompat.MediaItem> getAllArtists();

    /**
     * @return list of MediaItems
     */
    List<MediaBrowserCompat.MediaItem> getAllPlaylists();

    /**
     * @param mediaId mediaId of the song
     * @return list of QueueItem
     */
    List<MediaSessionCompat.QueueItem> getPlayingQueue(String mediaId);

    /**
     * @return list of QueueItem
     */
    List<MediaSessionCompat.QueueItem> getRandomQueue();

    /**
     * @param queueHint
     * @param mediaId
     * @return list of QueueItem
     */
    List<MediaSessionCompat.QueueItem> getQueue(int queueHint, String mediaId);

    List<MediaSessionCompat.QueueItem> getLastAddedQueue();

    List<MediaBrowserCompat.MediaItem> getPlaylistSongs(String mediaId);

    List<MediaSessionCompat.QueueItem> getPlaylistSongsQueue(String mediaId);

    interface QueueHint {
        int SINGLE_SONG = 0;
        int ARTIST_SONGS = 1;
        int ALBUM_SONGS = 2;
        int PLAYLIST_SONGS = 3;
    }
}
