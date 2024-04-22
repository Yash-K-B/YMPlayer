package com.yash.ymplayer.ui.custom;

import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.yash.ymplayer.PlayerService;

public abstract class ConnectionAwareFragment extends Fragment {

    private MediaBrowserCompat mediaBrowserCompat;
    private MediaControllerCompat mediaControllerCompat;
    private Context context;

    public ConnectionAwareFragment() {
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = requireContext();
        mediaBrowserCompat = new MediaBrowserCompat(context, new ComponentName(context, PlayerService.class), mConnectionCallbacks, null);
        mediaBrowserCompat.connect();
    }

    public abstract void onConnected(MediaControllerCompat mediaController);


    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {
        /**
         * Invoked after {@link MediaBrowser#connect()} when the request has successfully completed.
         */
        @Override
        public void onConnected() {
            mediaControllerCompat = new MediaControllerCompat(context, mediaBrowserCompat.getSessionToken());
            mediaControllerCompat.registerCallback(mMediaControllerCallbacks);
            ConnectionAwareFragment.this.onConnected(mediaControllerCompat);
        }
    };

    public MediaControllerCompat getMediaController() {
        return mediaControllerCompat;
    }

    public MediaControllerCompat.Callback mMediaControllerCallbacks = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }

        @Override
        public void onAudioInfoChanged(MediaControllerCompat.PlaybackInfo info) {
            super.onAudioInfoChanged(info);
        }
    };

    public MediaBrowserCompat getMediaBrowser() {
        return mediaBrowserCompat;
    }
}
