package com.yash.ymplayer.ui.spotify;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.yash.ymplayer.ui.main.SectionsPagerAdapter;

class SpotifyViewPagerAdapter extends FragmentStateAdapter {
    public SpotifyViewPagerAdapter(@NonNull FragmentManager fm, Lifecycle lifecycle) {
        super(fm, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        switch (position) {
            case 0:
                fragment = new SpotifyAlbums();
                break;
            case 1:
                fragment = new SpotifyAlbums();
                break;
            case 2:
                fragment = new SpotifyAlbums();
                break;
            case 3:
                fragment = new SpotifyAlbums();
                break;
            default:
                fragment = new SpotifyAlbums();
        }
        return fragment;

    }


    @Override
    public int getItemCount() {
        return 4;
    }
}
