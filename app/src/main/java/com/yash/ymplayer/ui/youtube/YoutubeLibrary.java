package com.yash.ymplayer.ui.youtube;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayoutMediator;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.interfaces.ActivityActionProvider;
import com.yash.ymplayer.R;
import com.yash.ymplayer.constant.Constants;
import com.yash.ymplayer.databinding.FragmentYoutubeLibraryBinding;
import com.yash.ymplayer.interfaces.EmbeddedListener;
import com.yash.ymplayer.repository.OnlineYoutubeRepository;
import com.yash.ymplayer.ui.youtube.search.YoutubeSearch;
import com.yash.youtube_extractor.models.YoutubePlaylist;
import com.yash.youtube_extractor.utility.CollectionUtility;

import java.util.List;
import java.util.Map;

public class YoutubeLibrary extends Fragment {
    private static final String TAG = "YoutubeLibrary";
    FragmentYoutubeLibraryBinding youtubeLibraryBinding;
    private Activity activity;
    public static String[] TAB_TITLES = {};

    public YoutubeLibrary() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);
        youtubeLibraryBinding = FragmentYoutubeLibraryBinding.inflate(inflater, container, false);
        youtubeLibraryBinding.btnRetry.setVisibility(View.VISIBLE);
        youtubeLibraryBinding.btnRetry.setOnClickListener(v -> loadLibrary(Constants.DEFAULT_CHANNEL));
        return youtubeLibraryBinding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = requireActivity();
        ((ActivityActionProvider) activity).setCustomToolbar(youtubeLibraryBinding.youtubeToolbar, "Youtube Library");
        loadLibrary(Constants.DEFAULT_CHANNEL);


    }

    private void loadLibrary(String channelId) {
        youtubeLibraryBinding.progressBar.setVisibility(View.VISIBLE);
        youtubeLibraryBinding.youtubeLibError.setVisibility(View.GONE);
        OnlineYoutubeRepository.getInstance(requireContext()).getChannelPlaylists(channelId, new OnlineYoutubeRepository.PlaylistLoadedCallback() {
            @Override
            public void onLoaded(Map<String, List<YoutubePlaylist>> playlistsByCategory) {
                if(getActivity() == null) {
                    LogHelper.d(TAG, "onLoaded: No Activity attached!!!");
                    return;
                }
                if (CollectionUtility.isEmpty(playlistsByCategory)) {
                    LogHelper.d(TAG, "Channel playlist empty");
                    showError();
                    return;
                }
                youtubeLibraryBinding.progressBar.setVisibility(View.GONE);
                youtubeLibraryBinding.youtubeLibError.setVisibility(View.GONE);
                TAB_TITLES = playlistsByCategory.keySet().toArray(new String[0]);
                YoutubeViewPagerAdapter adapter = new YoutubeViewPagerAdapter(getChildFragmentManager(), getLifecycle(), playlistsByCategory);
                youtubeLibraryBinding.youtubeViewPager.setAdapter(adapter);
                youtubeLibraryBinding.youtubeViewPager.registerOnPageChangeCallback(viewPagerPageChangeCallback);
                new TabLayoutMediator(youtubeLibraryBinding.tabs, youtubeLibraryBinding.youtubeViewPager, (tab, position) -> tab.setText(TAB_TITLES[position])).attach();
            }

            @Override
            public <E extends Exception> void onError(E e) {
                LogHelper.e(TAG, "Error while building YouTube Lib: ", e);
                showError();
            }
        });
    }

    private void showError() {
        youtubeLibraryBinding.youtubeLibError.setVisibility(View.VISIBLE);
        youtubeLibraryBinding.progressBar.setVisibility(View.GONE);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        youtubeLibraryBinding.youtubeViewPager.unregisterOnPageChangeCallback(viewPagerPageChangeCallback);
    }

    private final ViewPager2.OnPageChangeCallback viewPagerPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            ((EmbeddedListener)activity).onPageChange();
        }
    };
}