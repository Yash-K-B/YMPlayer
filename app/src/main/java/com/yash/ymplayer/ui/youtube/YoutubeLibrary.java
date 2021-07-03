package com.yash.ymplayer.ui.youtube;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.yash.ymplayer.ActivityActionProvider;
import com.yash.ymplayer.databinding.FragmentYoutubeLibraryBinding;

public class YoutubeLibrary extends Fragment {
    FragmentYoutubeLibraryBinding youtubeLibraryBinding;
    public static final String[] TAB_TITLES = {"TOP TRACKS","TODAY'S POPULAR","ALL TIME HIT"};
    public YoutubeLibrary() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        youtubeLibraryBinding = FragmentYoutubeLibraryBinding.inflate(inflater,container,false);
        return youtubeLibraryBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((ActivityActionProvider)getActivity()).setCustomToolbar(youtubeLibraryBinding.youtubeToolbar,"Youtube Library");
        YoutubeViewPagerAdapter adapter = new YoutubeViewPagerAdapter(getChildFragmentManager(),getLifecycle());
        youtubeLibraryBinding.youtubeViewPager.setAdapter(adapter);
        new TabLayoutMediator(youtubeLibraryBinding.tabs, youtubeLibraryBinding.youtubeViewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(TAB_TITLES[position]);
            }
        }).attach();
    }
}