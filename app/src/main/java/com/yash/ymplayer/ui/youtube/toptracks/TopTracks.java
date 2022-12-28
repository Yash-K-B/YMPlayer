package com.yash.ymplayer.ui.youtube.toptracks;

import android.content.ComponentName;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.databinding.FragmentTopTracksBinding;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.interfaces.TrackClickListener;
import com.yash.ymplayer.storage.AudioProvider;
import com.yash.ymplayer.ui.youtube.YoutubeLibraryViewModel;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.TrackContextMenuClickListener;
import com.yash.ymplayer.util.YoutubeSong;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TopTracks extends Fragment {
    private static final String TAG = "TopTracks";
    FragmentTopTracksBinding topTracksBinding;
    FragmentActivity activity;
    Context context;
    TopTracksAdapter adapter;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;
    private final Handler handler = new Handler(Looper.getMainLooper());
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    YoutubeLibraryViewModel viewModel;
    ConnectivityManager connectivityManager;
    boolean isContentLoaded = false;
    boolean isConnectedToService = false;
    boolean isNetworkAvailable = false;

    public TopTracks() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        topTracksBinding = FragmentTopTracksBinding.inflate(inflater, container, false);
        return topTracksBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        context = getContext();
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        topTracksBinding.topTracksContainer.setLayoutManager(new LinearLayoutManager(context));
        topTracksBinding.topTracksContainer.setHasFixedSize(true);
        mediaBrowser = new MediaBrowserCompat(context, new ComponentName(context, PlayerService.class), connectionCallback, null);
        mediaBrowser.connect();

    }

    @Override
    public void onStart() {
        super.onStart();
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
    }


    @Override
    public void onStop() {
        super.onStop();
        if (connectivityManager != null)
            connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaBrowser.disconnect();
        if (connectivityManager != null)
            connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            super.onConnected();
            viewModel = new ViewModelProvider(TopTracks.this).get(YoutubeLibraryViewModel.class);
            mediaController = new MediaControllerCompat(context, mediaBrowser.getSessionToken());
            LogHelper.d(TAG, "onConnected: TopTracks");
            isConnectedToService = true;
            handler.postDelayed(noInternet, 1000);
            adapter = new TopTracksAdapter(context, new TrackContextMenuClickListener(context, mediaController, "TOP_TRACKS|"), mediaController);
            topTracksBinding.topTracksContainer.setAdapter(adapter);

            if (!isContentLoaded && isNetworkAvailable)
                loadContent();

        }

    };
    NetworkRequest networkRequest = new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET).build();

    final NetworkCallback networkCallback = new NetworkCallback() {

        @Override
        public void onAvailable(@NonNull Network network) {
            LogHelper.d(TAG, "onAvailable: ");
            isNetworkAvailable = true;
            handler.removeCallbacks(noInternet);
            handler.post(() -> {
                if (isContentLoaded) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        Snackbar.make(topTracksBinding.getRoot(), Html.fromHtml("<font color='green'>Internet Connection Available</font>", Html.FROM_HTML_MODE_LEGACY), Snackbar.LENGTH_SHORT).show();
                    else
                        Snackbar.make(topTracksBinding.getRoot(), Html.fromHtml("<font color='green'>Internet Connection Available</font>"), Snackbar.LENGTH_SHORT).show();
                } else {
                    loadContent();
                }
            });
        }


        @Override
        public void onLost(@NonNull Network network) {
            LogHelper.d(TAG, "onLost: ");
            isNetworkAvailable = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Snackbar.make(topTracksBinding.getRoot(), Html.fromHtml("<font color='red'>Internet Connection Lost</font>", Html.FROM_HTML_MODE_LEGACY), Snackbar.LENGTH_INDEFINITE).show();
            else
                Snackbar.make(topTracksBinding.getRoot(), Html.fromHtml("<font color='red'>Internet Connection Lost</font>"), Snackbar.LENGTH_INDEFINITE).show();
        }
    };


    void loadContent() {

        if (isConnectedToService && isNetworkAvailable) {
            handler.removeCallbacks(noInternet);
            topTracksBinding.topTracksLoadingHint.setVisibility(View.VISIBLE);
            topTracksBinding.noInternetHint.setVisibility(View.INVISIBLE);
            isContentLoaded = true;
            viewModel.getTopTracks().observe(getViewLifecycleOwner(), youtubeSongs -> {
                LogHelper.d(TAG, "onChanged: TopTracks : " + youtubeSongs.isEmpty());
                adapter.submitList(youtubeSongs);
            });
        }

    }

    Runnable noInternet = () -> {
        topTracksBinding.topTracksLoadingHint.setVisibility(View.INVISIBLE);
        topTracksBinding.noInternetHint.setVisibility(View.VISIBLE);
    };
}