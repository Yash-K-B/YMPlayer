package com.yash.ymplayer.ui.youtube.alltimehit;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yash.ymplayer.PlaylistExpandActivity;
import com.yash.ymplayer.databinding.FragmentAllTimeHitBinding;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.models.PopularPlaylist;
import com.yash.ymplayer.ui.youtube.YoutubeLibraryViewModel;
import com.yash.ymplayer.ui.youtube.adapters.PopularHitAdapter;
import com.yash.ymplayer.interfaces.Keys;

import java.util.ArrayList;
import java.util.List;

public class AllTimeHit extends Fragment {
    private static final String TAG = "AllTimeHit";
    FragmentAllTimeHitBinding allTimeHitBinding;
    FragmentActivity activity;
    Context context;
    MediaBrowserCompat mediaBrowser;
    List<PopularPlaylist> playlists = new ArrayList<>();
    PopularHitAdapter adapter;
    MediaControllerCompat mediaController;
    YoutubeLibraryViewModel viewModel;
    private boolean isContentLoaded;

    public AllTimeHit() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        allTimeHitBinding = FragmentAllTimeHitBinding.inflate(inflater, container, false);
        return allTimeHitBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        context = getContext();
        activity = getActivity();
        adapter = new PopularHitAdapter(context, playlists, (v, playlist) -> {
            Intent intent = new Intent(context, PlaylistExpandActivity.class);
            intent.putExtra(Keys.EXTRA_PARENT_ID, playlist.getId());
            intent.putExtra(Keys.EXTRA_TITLE, playlist.getSnippet().getLocalized().getTitle());
            String url = playlist.getSnippet().getThumbnails().getStandard() == null ? playlist.getSnippet().getThumbnails().getMedium().getUrl() : playlist.getSnippet().getThumbnails().getStandard().getUrl();
            intent.putExtra(Keys.EXTRA_ART_URL, url);
            //ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity,v.getRoot(),"list_name");
            context.startActivity(intent);
        });
        allTimeHitBinding.allTimeHitContainer.setAdapter(adapter);
        allTimeHitBinding.allTimeHitContainer.setLayoutManager(new LinearLayoutManager(context));
        allTimeHitBinding.allTimeHitContainer.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        viewModel = new ViewModelProvider(activity).get(YoutubeLibraryViewModel.class);
        viewModel.getAllTimeHitPlaylist().observe(getViewLifecycleOwner(), new Observer<List<PopularPlaylist>>() {
            @Override
            public void onChanged(List<PopularPlaylist> playlists) {
                allTimeHitBinding.allTimeHitProgress.setVisibility(View.GONE);
                if (playlists == null) {
                    LogHelper.d(TAG, "onChanged: Error");
                    allTimeHitBinding.allTimeHitError.setVisibility(View.VISIBLE);
                    return;
                }
                allTimeHitBinding.allTimeHitError.setVisibility(View.GONE);
                AllTimeHit.this.playlists.clear();
                AllTimeHit.this.playlists.addAll(playlists);
                adapter.notifyDataSetChanged();
            }
        });
        allTimeHitBinding.btnRetry.setOnClickListener(v -> {
            allTimeHitBinding.allTimeHitProgress.setVisibility(View.VISIBLE);
            allTimeHitBinding.allTimeHitError.setVisibility(View.GONE);
            viewModel.refreshAllTimeHit();
        });
    }


}