package com.yash.ymplayer.ui.youtube.discover;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.PlaylistExpandActivity;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentDiscoverNewBinding;
import com.yash.ymplayer.models.PopularPlaylist;
import com.yash.ymplayer.ui.youtube.YoutubeLibraryViewModel;
import com.yash.ymplayer.ui.youtube.magic90s.Magic90s;
import com.yash.ymplayer.ui.youtube.todayspopular.PopularHitAdapter;
import com.yash.ymplayer.util.Keys;

import java.util.ArrayList;
import java.util.List;

public class DiscoverNew extends Fragment {
    private static final String TAG = "DiscoverNew";
    FragmentDiscoverNewBinding discoverNewBinding;
    Context context;
    FragmentActivity activity;
    PopularHitAdapter adapter;
    List<PopularPlaylist> playlists = new ArrayList<>();
    YoutubeLibraryViewModel viewModel;

    public DiscoverNew() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        discoverNewBinding = FragmentDiscoverNewBinding.inflate(inflater, container, false);
        return discoverNewBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getContext();
        activity = getActivity();
        adapter = new PopularHitAdapter(context, playlists, (v, playlist) -> {
            Intent intent = new Intent(context, PlaylistExpandActivity.class);
            intent.putExtra(Keys.EXTRA_PARENT_ID, playlist.getId());
            intent.putExtra(Keys.EXTRA_TITLE, playlist.getSnippet().getLocalized().getTitle());
            intent.putExtra(Keys.EXTRA_ART_URL, playlist.getSnippet().getThumbnails().getStandard().getUrl());
            //ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity,v.getRoot(),"list_name");
            context.startActivity(intent);
        });
        discoverNewBinding.discoverNewContainer.setAdapter(adapter);
        discoverNewBinding.discoverNewContainer.setLayoutManager(new LinearLayoutManager(context));
        discoverNewBinding.discoverNewContainer.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        viewModel = new ViewModelProvider(activity).get(YoutubeLibraryViewModel.class);
        viewModel.getDiscoverNewMusic().observe(getViewLifecycleOwner(), new Observer<List<PopularPlaylist>>() {
            @Override
            public void onChanged(List<PopularPlaylist> playlists) {
                discoverNewBinding.discoverNewProgress.setVisibility(View.GONE);
                if (playlists == null) {
                    LogHelper.d(TAG, "onChanged: Error");
                    discoverNewBinding.discoverNewError.setVisibility(View.VISIBLE);
                    return;
                }
                discoverNewBinding.discoverNewError.setVisibility(View.GONE);
                DiscoverNew.this.playlists.clear();
                DiscoverNew.this.playlists.addAll(playlists);
                adapter.notifyDataSetChanged();
            }
        });
        discoverNewBinding.btnRetry.setOnClickListener(v -> {
            discoverNewBinding.discoverNewProgress.setVisibility(View.VISIBLE);
            discoverNewBinding.discoverNewError.setVisibility(View.GONE);
            viewModel.refreshDiscoverNewMusic();
        });
    }
}