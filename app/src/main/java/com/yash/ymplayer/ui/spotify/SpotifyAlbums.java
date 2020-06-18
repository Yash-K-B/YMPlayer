package com.yash.ymplayer.ui.spotify;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentSpotifyAlbumsBinding;
import com.yash.ymplayer.util.AlbumListAdapter;
import com.yash.ymplayer.util.SongListAdapter;
import com.yash.ymplayer.util.SongsListAdapter;

import java.util.ArrayList;
import java.util.List;

public class SpotifyAlbums extends Fragment {
    FragmentSpotifyAlbumsBinding spotifyAlbumsBinding;
    Context context;
    FragmentActivity activity;
    private List<MediaBrowserCompat.MediaItem> songs = new ArrayList<>();

    public SpotifyAlbums() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        spotifyAlbumsBinding = FragmentSpotifyAlbumsBinding.inflate(inflater, container, false);
        return spotifyAlbumsBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getContext();
        activity = getActivity();
        AlbumListAdapter adapter = new AlbumListAdapter(context, songs, new AlbumListAdapter.OnItemClickListener() {
            @Override
            public void onClick(MediaBrowserCompat.MediaItem song, long id) {

            }
        });
        spotifyAlbumsBinding.spotifyAlbumContainer.setAdapter(adapter);
        spotifyAlbumsBinding.spotifyAlbumContainer.setLayoutManager(new LinearLayoutManager(context));
        spotifyAlbumsBinding.spotifyAlbumContainer.addItemDecoration(new DividerItemDecoration(context,DividerItemDecoration.VERTICAL));


    }


}