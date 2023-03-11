package com.yash.ymplayer.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import com.yash.ymplayer.download.manager.DownloadService;
import com.yash.ymplayer.interfaces.TrackClickListener;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.storage.AudioProvider;

import java.util.ArrayList;
import java.util.List;

public class TrackContextMenuClickListener implements TrackClickListener {

    private MediaControllerCompat mediaController;
    private Context context;
    private String playlistPrefix;

    public TrackContextMenuClickListener(Context context, MediaControllerCompat mediaController, String playlistPrefix) {
        this.mediaController = mediaController;
        this.context = context;
        this.playlistPrefix = playlistPrefix;
    }

    @Override
    public void onClick(YoutubeSong song) {
        String audioUri = playlistPrefix + song.getVideoId();
        mediaController.getTransportControls().playFromMediaId(audioUri, null);
    }

    @Override
    public void onPlaySingle(YoutubeSong song) {
        Bundle extra = new Bundle();
        extra.putBoolean(Keys.PLAY_SINGLE, true);
        mediaController.getTransportControls().playFromMediaId(playlistPrefix + song.getVideoId(), extra);
    }

    @Override
    public void onQueueNext(YoutubeSong song) {
        Bundle extras = new Bundle();
        extras.putString(Keys.MEDIA_ID, playlistPrefix + song.getVideoId());
        extras.putInt(Keys.QUEUE_HINT, AudioProvider.QueueHint.YOUTUBE_SINGLE_SONG);
        extras.putString(Keys.QUEUE_MODE, Keys.QueueMode.ONLINE.name());
        mediaController.getTransportControls().sendCustomAction(Keys.Action.QUEUE_NEXT, extras);
    }

    @Override
    public void onQueueLast(YoutubeSong song) {
        Bundle extras = new Bundle();
        extras.putString(Keys.MEDIA_ID, playlistPrefix + song.getVideoId());
        extras.putInt(Keys.QUEUE_HINT, AudioProvider.QueueHint.YOUTUBE_SINGLE_SONG);
        extras.putString(Keys.QUEUE_MODE, Keys.QueueMode.ONLINE.name());
        mediaController.getTransportControls().sendCustomAction(Keys.Action.QUEUE_LAST, extras);
    }

    @Override
    public void addToPlaylist(YoutubeSong song) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose Playlist");
        List<MediaBrowserCompat.MediaItem> lists = Repository.getInstance(context).getAllPlaylists();
        List<String> playlistNames = new ArrayList<>();
        for (int i = 0; i < lists.size(); i++) {
            if(lists.get(i).getDescription().getDescription() == null)
                continue;
            playlistNames.add(lists.get(i).getDescription().getTitle().toString());
        }
        builder.setItems(playlistNames.toArray(new String[0]), (dialog, which) -> {
            String playlistName = playlistNames.get(which);
            Bundle extras = new Bundle();
            extras.putString(Keys.MEDIA_ID, song.getVideoId());
            extras.putString(Keys.TITLE, song.getTitle());
            extras.putString(Keys.ARTIST, song.getChannelTitle());
            extras.putString(Keys.ARTWORK, song.getArt_url_high());
            extras.putString(Keys.ALBUM, "YMPlayer");
            extras.putString(Keys.PLAYLIST_NAME, playlistName);
            mediaController.getTransportControls().sendCustomAction(Keys.Action.ADD_TO_PLAYLIST, extras);
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void download(YoutubeSong song, int bitRateInKbps) {
        Intent downloadIntent = new Intent(context, DownloadService.class);
        downloadIntent.putExtra(Keys.VIDEO_ID,song.getVideoId());
        downloadIntent.putExtra(Keys.EXTRA_DOWNLOAD_QUALITY,bitRateInKbps);
        context.startService(downloadIntent);
    }
}
