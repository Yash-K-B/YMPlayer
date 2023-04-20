package com.yash.ymplayer.ui.youtube.todayspopular;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.PlaylistExpandActivity;
import com.yash.ymplayer.databinding.FragmentPopularHitBinding;
import com.yash.ymplayer.models.PopularPlaylist;
import com.yash.ymplayer.ui.youtube.YoutubeLibraryViewModel;
import com.yash.ymplayer.ui.youtube.adapters.PopularHitAdapter;
import com.yash.ymplayer.interfaces.Keys;

import java.util.ArrayList;
import java.util.List;

public class PopularHit extends Fragment {
    private static final String TAG = "PopularHit";
    FragmentPopularHitBinding popularHitBinding;
    FragmentActivity activity;
    Context context;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;
    PopularHitAdapter adapter;
    List<PopularPlaylist> playlists = new ArrayList<>();
    YoutubeLibraryViewModel viewModel;

    public PopularHit() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        popularHitBinding = FragmentPopularHitBinding.inflate(inflater, container, false);
        return popularHitBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getContext();
        activity = getActivity();

        adapter = new PopularHitAdapter(context, playlists, (v, playlist) -> {
            Intent intent = new Intent(context, PlaylistExpandActivity.class);
            intent.putExtra(Keys.EXTRA_PARENT_ID,playlist.getId());
            intent.putExtra(Keys.EXTRA_TITLE,playlist.getSnippet().getLocalized().getTitle());
            intent.putExtra(Keys.EXTRA_ART_URL,playlist.getSnippet().getThumbnails().getStandard().getUrl());
            context.startActivity(intent);
        });
        popularHitBinding.popularHitContainer.setAdapter(adapter);
        popularHitBinding.popularHitContainer.setLayoutManager(new LinearLayoutManager(context));
        popularHitBinding.popularHitContainer.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        viewModel = new ViewModelProvider(activity).get(YoutubeLibraryViewModel.class);
        viewModel.getPopularPlaylist().observe(getViewLifecycleOwner(), playlists -> {
            popularHitBinding.popularHitProgress.setVisibility(View.GONE);
            if (playlists == null) {
                LogHelper.d(TAG, "onConnected: error");
                popularHitBinding.popularHitError.setVisibility(View.VISIBLE);
                return;
            }
            LogHelper.d(TAG, "onActivityCreated: Playlist size : "+playlists.size());
            popularHitBinding.popularHitError.setVisibility(View.GONE);
            PopularHit.this.playlists.clear();
            PopularHit.this.playlists.addAll(playlists);
            adapter.notifyDataSetChanged();
        });
        popularHitBinding.btnRetry.setOnClickListener(v -> {
            popularHitBinding.popularHitProgress.setVisibility(View.VISIBLE);
            popularHitBinding.popularHitError.setVisibility(View.GONE);
            viewModel.refreshPopularHit();
        });

    }


}