package com.yash.ymplayer.util;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;


import androidx.annotation.NonNull;

import com.yash.ymplayer.interfaces.Keys;
import com.yash.youtube_extractor.models.YoutubePlaylist;
import com.yash.youtube_extractor.utility.CommonUtility;

import java.util.ArrayList;
import java.util.List;

public class MediaItemHelperUtility {
    private MediaItemHelperUtility() {}

    public static Bundle createBundle(YoutubeSong song) {
        Bundle extras = new Bundle();
        extras.putString(Keys.MEDIA_ID, song.getVideoId());
        extras.putString(Keys.TITLE, song.getTitle());
        extras.putString(Keys.ARTIST, song.getChannelTitle());
        extras.putString(Keys.ARTWORK, song.getArt_url_high());
        extras.putString(Keys.ALBUM, "YMPlayer");
        return extras;
    }


    public static MediaBrowserCompat.MediaItem toMediaItems(String channel, String prefix) {
        return new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId( prefix + "/" + channel)
                    .setTitle(channel)
                    .setSubtitle("")
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }
    public static List<MediaBrowserCompat.MediaItem> toMediaItems(List<YoutubePlaylist> list, String prefix) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (YoutubePlaylist playlist : list) {
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId( prefix + "/" + playlist.getPlaylistId())
                    .setMediaUri(Uri.parse(playlist.getPlaylistId()))
                    .setTitle(playlist.getTitle())
                    .setSubtitle(playlist.getDescription())
                    .setIconUri(Uri.parse(playlist.getArtUrlMedium()))
                    .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
            mediaItems.add(item);
        }
        return mediaItems;
    }

    public static List<MediaBrowserCompat.MediaItem> mapToMediaItems(List<YoutubeSong> list, String prefix) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        for (YoutubeSong song : list) {
            MediaBrowserCompat.MediaItem item = new MediaBrowserCompat.MediaItem(new MediaDescriptionCompat.Builder()
                    .setMediaId( prefix + "|" + song.getVideoId())
                    .setMediaUri(Uri.parse(song.getVideoId()))
                    .setTitle(song.getTitle())
                    .setSubtitle(song.getChannelTitle())
                    .setIconUri(Uri.parse(song.getArt_url_medium()))
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            mediaItems.add(item);
        }
        return mediaItems;
    }

    public static List<MediaSessionCompat.QueueItem> toQueueItems(List<com.yash.youtube_extractor.models.YoutubeSong> list) {
        List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
        if(list == null)
            return mediaItems;
        long i = System.currentTimeMillis();
        for(com.yash.youtube_extractor.models.YoutubeSong song: list) {
            Bundle extras = new Bundle();
            extras.putLong("duration", song.getDurationMillis());
            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                    .setMediaId(song.getVideoId())
                    .setTitle(song.getTitle())
                    .setSubtitle(song.getChannelTitle())
                    .setDescription(song.getChannelDesc())
                    .setIconUri(Uri.parse(song.getArtUrlMedium()))
                    .setExtras(extras)
                    .build(), ++i);
            mediaItems.add(queueItem);
        }
        return mediaItems;
    }

    public static List<MediaSessionCompat.QueueItem> getQueueFrom(Bundle extras) {
        List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
        if(extras == null || !extras.containsKey(Keys.MEDIA_ID))
            return mediaItems;
        MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                .setMediaId(extras.getString(Keys.MEDIA_ID))
                .setTitle(extras.getString(Keys.TITLE))
                .setSubtitle(extras.getString(Keys.ARTIST))
                .setDescription(extras.getString(Keys.ALBUM))
                .setIconUri(Uri.parse(extras.getString(Keys.ARTWORK)))
                .build(), 0);
        mediaItems.add(queueItem);
        return mediaItems;
    }
}
