package com.yash.ymplayer.ui.youtube;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.yash.ymplayer.ActivityActionProvider;
import com.yash.ymplayer.R;
import com.yash.ymplayer.SearchActivity;
import com.yash.ymplayer.databinding.FragmentYoutubeLibraryBinding;
import com.yash.ymplayer.ui.youtube.search.YoutubeSearch;

public class YoutubeLibrary extends Fragment {
    private static final String TAG = "YoutubeLibrary";
    FragmentYoutubeLibraryBinding youtubeLibraryBinding;
    public static final String[] TAB_TITLES = {"TOP TRACKS","TODAY'S POPULAR","DISCOVER NEW","ALL TIME HIT","90s MAGIC"};
    public YoutubeLibrary() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);
        youtubeLibraryBinding = FragmentYoutubeLibraryBinding.inflate(inflater,container,false);
        return youtubeLibraryBinding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ActivityActionProvider)getActivity()).setCustomToolbar(youtubeLibraryBinding.youtubeToolbar,"Youtube Library");
        YoutubeViewPagerAdapter adapter = new YoutubeViewPagerAdapter(getChildFragmentManager(),getLifecycle());
        youtubeLibraryBinding.youtubeViewPager.setAdapter(adapter);
        new TabLayoutMediator(youtubeLibraryBinding.tabs, youtubeLibraryBinding.youtubeViewPager, (tab, position) -> tab.setText(TAB_TITLES[position])).attach();

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.app_menu_extention_search, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: YoutubeLibrary");
        if (item.getItemId() == R.id.search) {
            startActivity(new Intent(getContext(), YoutubeSearch.class));
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }
}