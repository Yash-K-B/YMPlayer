package com.yash.ymplayer.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.annotation.NonNull;

import com.yash.ymplayer.download.manager.DownloadService;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.interfaces.TrackClickListener;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.interfaces.AudioProvider;

import java.util.ArrayList;
import java.util.List;

public class TrackContextMenuClickListener implements TrackClickListener {

    private final MediaControllerCompat mediaController;
    private final Context context;
    private final String playlistPrefix;

    public TrackContextMenuClickListener(Context context, MediaControllerCompat mediaController, String playlistPrefix) {
        this.mediaController = mediaController;
        this.context = context;
        this.playlistPrefix = playlistPrefix;
    }

    @Override
    public void onClick(YoutubeSong song) {
        String audioUri = playlistPrefix + song.getVideoId();
        mediaController.getTransportControls().playFromMediaId(audioUri, MediaItemHelperUtility.createBundle(song));
    }

    @Override
    public void onPlaySingle(YoutubeSong song) {
        Bundle extras = MediaItemHelperUtility.createBundle(song);
        extras.putBoolean(Keys.PLAY_SINGLE, true);
        mediaController.getTransportControls().playFromMediaId(playlistPrefix + song.getVideoId(), extras);
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
            if (lists.get(i).getDescription().getDescription() == null)
                continue;
            playlistNames.add(lists.get(i).getDescription().getTitle().toString());
        }
        builder.setItems(playlistNames.toArray(new String[0]), (dialog, which) -> {
            String playlistName = playlistNames.get(which);
            Bundle extras = MediaItemHelperUtility.createBundle(song);
            extras.putString(Keys.PLAYLIST_NAME, playlistName);
            mediaController.getTransportControls().sendCustomAction(Keys.Action.ADD_TO_PLAYLIST, extras);
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void download(YoutubeSong song, int bitRateInKbps) {
        Intent downloadIntent = new Intent(context, DownloadService.class);
        downloadIntent.putExtra(Keys.DownloadManager.EXTRA_VIDEO_ID, song.getVideoId());
        downloadIntent.putExtra(Keys.DownloadManager.EXTRA_BITRATE, bitRateInKbps);
        context.startService(downloadIntent);
    }
}
