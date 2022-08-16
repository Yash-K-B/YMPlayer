package com.yash.ymplayer.ui.youtube;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.yash.ymplayer.ui.youtube.alltimehit.AllTimeHit;
import com.yash.ymplayer.ui.youtube.category.PlaylistsViewer;
import com.yash.ymplayer.ui.youtube.discover.DiscoverNew;
import com.yash.ymplayer.ui.youtube.magic90s.Magic90s;
import com.yash.ymplayer.ui.youtube.todayspopular.PopularHit;
import com.yash.ymplayer.ui.youtube.toptracks.TopTracks;
import com.yash.youtube_extractor.models.YoutubePlaylist;

import static com.yash.ymplayer.ui.youtube.YoutubeLibrary.TAB_TITLES;

import android.util.Pair;

import java.util.List;

public class YoutubeViewPagerAdapter extends FragmentStateAdapter {

    private final List<Pair<String, List<YoutubePlaylist>>> playlistsByCategory;

    public YoutubeViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, List<Pair<String, List<YoutubePlaylist>>> playlistsByCategory) {
        super(fragmentManager, lifecycle);
        this.playlistsByCategory = playlistsByCategory;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new TopTracks();
                break;
            default:
                fragment = new PlaylistsViewer(playlistsByCategory.get(position - 1).second);
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return playlistsByCategory.size() == 0 ? 0 : playlistsByCategory.size() + 1;
    }
}
