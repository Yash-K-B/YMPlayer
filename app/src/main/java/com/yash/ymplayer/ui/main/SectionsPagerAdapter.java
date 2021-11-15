package com.yash.ymplayer.ui.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "debug";

    public static SectionsPagerAdapter instance;

    public static SectionsPagerAdapter getInstance(@NonNull FragmentManager fm,Lifecycle lifecycle){
        if(instance == null)
            instance = new SectionsPagerAdapter(fm, lifecycle);
        return instance;
    }

    public SectionsPagerAdapter(@NonNull FragmentManager fm,Lifecycle lifecycle) {
        super(fm,lifecycle);
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {

        switch (position){
            case 0:
                return new AllSongs();
                //break;
            case 1:
                return new Albums();
               // break;
            case 2:
                return new Artists();
               // break;
            case 3:
                return new Playlists();
               // break;
            default:
                return new AllSongs();
        }
       // return fragment;
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}