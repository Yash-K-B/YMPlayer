package com.yash.ymplayer.util;

import android.net.Uri;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;


import com.yash.youtube_extractor.models.YoutubePlaylist;

import java.util.ArrayList;
import java.util.List;

public class MediaItemHelperUtility {
    private MediaItemHelperUtility() {}


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
}
