package com.yash.ymplayer.ui.main;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.yash.ymplayer.ActivityActionProvider;
import com.yash.ymplayer.databinding.CreatePlaylistBinding;
import com.yash.ymplayer.databinding.FragmentLocalSongsBinding;
import com.yash.ymplayer.repository.Repository;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocalSongs extends Fragment{
    private static final String[] TAB_TITLES = new String[]{"All Songs", "Albums", "Artists", "Playlist"};
    private static final String TAG = "debug";
    FragmentLocalSongsBinding binding;
    public static LocalSongs instance;

    public LocalSongs() {
        // Required empty public constructor
    }

    public static LocalSongs getInstance() {
        if (instance == null)
            instance = new LocalSongs();
        return instance;
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLocalSongsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated: LocalSongs");
        ((ActivityActionProvider) getActivity()).setCustomToolbar(binding.toolbar, "Local Library");
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(),getActivity().getLifecycle());
        binding.floatingActionButton.hide();
        binding.viewPager.setAdapter(sectionsPagerAdapter);
        new TabLayoutMediator(binding.tabs, binding.viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                tab.setText(TAB_TITLES[position]);
            }
        }).attach();
        binding.viewPager.registerOnPageChangeCallback(viewPagerPageChangeCallback);
        binding.viewPager.setOffscreenPageLimit(1);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.viewPager.unregisterOnPageChangeCallback(viewPagerPageChangeCallback);
    }

    private ViewPager2.OnPageChangeCallback viewPagerPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            if (position == 3) {
                binding.floatingActionButton.show();
            } else {
                binding.floatingActionButton.hide();
            }
        }
    };


    public void onFabClicked(Playlists playlist){
        binding.floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                CreatePlaylistBinding playlistBinding = CreatePlaylistBinding.inflate(getLayoutInflater());
                builder.setTitle("Create playlist")
                        .setView(playlistBinding.getRoot())
                        .setPositiveButton("SAVE", (dialog, which) -> {
                            if(Repository.getInstance(getContext()).createPlaylist(playlistBinding.playlistName.getText().toString()) == -1){
                                Toast.makeText(getContext(), "Playlist already exists", Toast.LENGTH_SHORT).show();
                            };
                            playlist.onChanged();
                        })
                        .setNegativeButton("CANCEL", (dialog, which) -> { dialog.dismiss(); });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }
}

