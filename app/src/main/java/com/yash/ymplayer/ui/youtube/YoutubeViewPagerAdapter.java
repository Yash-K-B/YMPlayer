package com.yash.ymplayer.ui.youtube;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.yash.ymplayer.ui.youtube.alltimehit.AllTimeHit;
import com.yash.ymplayer.ui.youtube.discover.DiscoverNew;
import com.yash.ymplayer.ui.youtube.magic90s.Magic90s;
import com.yash.ymplayer.ui.youtube.todayspopular.PopularHit;
import com.yash.ymplayer.ui.youtube.toptracks.TopTracks;

import static com.yash.ymplayer.ui.youtube.YoutubeLibrary.TAB_TITLES;

public class YoutubeViewPagerAdapter extends FragmentStateAdapter {

    public YoutubeViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 1:
                fragment = new PopularHit();
                break;
            case 2:
                fragment = new DiscoverNew();
                break;
            case 3:
                fragment = new AllTimeHit();
                break;
            case 4:
                fragment = new Magic90s();
                break;
            default:
                fragment = new TopTracks();
        }
        return fragment;
    }

    @Override
    public int getItemCount() {
        return TAB_TITLES.length;
    }
}
