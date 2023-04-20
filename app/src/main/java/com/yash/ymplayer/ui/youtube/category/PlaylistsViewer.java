package com.yash.ymplayer.ui.youtube.category;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yash.ymplayer.PlaylistExpandActivity;
import com.yash.ymplayer.databinding.FragmentMagic90sBinding;
import com.yash.ymplayer.ui.youtube.YoutubeLibraryViewModel;
import com.yash.ymplayer.ui.youtube.adapters.PlaylistAdapter;
import com.yash.ymplayer.interfaces.Keys;

import java.util.List;

public class PlaylistsViewer extends Fragment {

    private static final String TAG = "PlaylistsViewer";

    Context context;
    FragmentActivity activity;

    PlaylistAdapter adapter;
    List<com.yash.youtube_extractor.models.YoutubePlaylist> playlists;
    YoutubeLibraryViewModel viewModel;
    FragmentMagic90sBinding magic90sBinding;

    public PlaylistsViewer(List<com.yash.youtube_extractor.models.YoutubePlaylist> playlists) {
        this.playlists = playlists;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        magic90sBinding = FragmentMagic90sBinding.inflate(inflater, container, false);
        return magic90sBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = requireContext();
        activity = requireActivity();
        adapter = new PlaylistAdapter(context, playlists, (v, playlist) -> {
            Intent intent = new Intent(context, PlaylistExpandActivity.class);
            intent.putExtra(Keys.EXTRA_PARENT_ID, playlist.getPlaylistId());
            intent.putExtra(Keys.EXTRA_TITLE, playlist.getTitle());
            intent.putExtra(Keys.EXTRA_ART_URL, playlist.getArtUrlMedium());
            context.startActivity(intent);
        });
        magic90sBinding.magic90sContainer.setAdapter(adapter);
        magic90sBinding.magic90sContainer.setLayoutManager(new LinearLayoutManager(context));
        magic90sBinding.magic90sContainer.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        magic90sBinding.magic90sProgress.setVisibility(View.GONE);
        magic90sBinding.magic90sError.setVisibility(View.GONE);
        magic90sBinding.btnRetry.setOnClickListener(v -> {
            magic90sBinding.magic90sProgress.setVisibility(View.VISIBLE);
            magic90sBinding.magic90sError.setVisibility(View.GONE);
            viewModel.refresh90sMagic();
        });
    }
}