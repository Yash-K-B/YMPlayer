package com.yash.ymplayer.ui.youtube.magic90s;

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
import com.yash.ymplayer.databinding.FragmentPlaylistViewerBinding;
import com.yash.ymplayer.models.PopularPlaylist;
import com.yash.ymplayer.ui.youtube.YoutubeLibraryViewModel;
import com.yash.ymplayer.ui.youtube.adapters.PopularHitAdapter;
import com.yash.ymplayer.interfaces.Keys;

import java.util.ArrayList;
import java.util.List;

public class Magic90s extends Fragment {

    private static final String TAG = "magic90s";

    Context context;
    FragmentActivity activity;

    PopularHitAdapter adapter;
    List<PopularPlaylist> playlists = new ArrayList<>();
    YoutubeLibraryViewModel viewModel;
    FragmentPlaylistViewerBinding magic90sBinding;

    public Magic90s() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        magic90sBinding = FragmentPlaylistViewerBinding.inflate(inflater,container, false);
        return magic90sBinding.getRoot();
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
        magic90sBinding.listRv.setAdapter(adapter);
        magic90sBinding.listRv.setLayoutManager(new LinearLayoutManager(context));
        magic90sBinding.listRv.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        viewModel = new ViewModelProvider(activity).get(YoutubeLibraryViewModel.class);
        viewModel.get90sMagic().observe(getViewLifecycleOwner(), new Observer<List<PopularPlaylist>>() {
            @Override
            public void onChanged(List<PopularPlaylist> playlists) {
                magic90sBinding.playlistProgress.setVisibility(View.GONE);
                if (playlists == null) {
                    LogHelper.d(TAG, "onChanged: Error");
                    magic90sBinding.playlistError.setVisibility(View.VISIBLE);
                    return;
                }
                magic90sBinding.playlistError.setVisibility(View.GONE);
                Magic90s.this.playlists.clear();
                Magic90s.this.playlists.addAll(playlists);
                adapter.notifyDataSetChanged();
            }
        });
        magic90sBinding.btnRetry.setOnClickListener(v -> {
            magic90sBinding.playlistProgress.setVisibility(View.VISIBLE);
            magic90sBinding.playlistError.setVisibility(View.GONE);
            viewModel.refresh90sMagic();
        });
    }
}