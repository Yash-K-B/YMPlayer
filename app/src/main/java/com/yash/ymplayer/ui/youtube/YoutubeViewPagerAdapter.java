package com.yash.ymplayer.ui.youtube;

import static com.yash.ymplayer.ui.youtube.YoutubeLibrary.TAB_TITLES;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.yash.ymplayer.ui.youtube.category.PlaylistsViewer;
import com.yash.youtube_extractor.models.YoutubePlaylist;

import java.util.List;
import java.util.Map;

public class YoutubeViewPagerAdapter extends FragmentStateAdapter {

    private final Map<String, List<YoutubePlaylist>> playlistsByCategory;

    public YoutubeViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, Map<String, List<YoutubePlaylist>> playlistsByCategory) {
        super(fragmentManager, lifecycle);
        this.playlistsByCategory = playlistsByCategory;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return new PlaylistsViewer(playlistsByCategory.get(TAB_TITLES[position]));
    }

    @Override
    public int getItemCount() {
        return playlistsByCategory.size();
    }
}
