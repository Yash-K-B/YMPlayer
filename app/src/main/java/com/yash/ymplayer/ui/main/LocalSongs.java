package com.yash.ymplayer.ui.main;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.yash.ymplayer.interfaces.ActivityActionProvider;
import com.yash.ymplayer.R;
import com.yash.ymplayer.SearchActivity;
import com.yash.ymplayer.databinding.CreatePlaylistBinding;
import com.yash.ymplayer.databinding.FragmentLocalSongsBinding;
import com.yash.ymplayer.interfaces.EmbeddedListener;
import com.yash.ymplayer.repository.Repository;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocalSongs extends Fragment {
    private static final String[] TAB_TITLES = new String[]{"All Songs", "Albums", "Artists", "Playlist"};
    private static final String TAG = "debug";
    FragmentLocalSongsBinding binding;
    Context context;
    Activity activity;

    public LocalSongs() {
        // Required empty public constructor
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLocalSongsBinding.inflate(inflater, container, false);
        setHasOptionsMenu(true);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onActivityCreated: LocalSongs");
        context = requireContext();
        activity = requireActivity();
        ((ActivityActionProvider) activity).setCustomToolbar(binding.toolbar, "Local Library");
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(), getActivity().getLifecycle());
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

    private final ViewPager2.OnPageChangeCallback viewPagerPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            ((EmbeddedListener)activity).onPageChange();
            if (position == 3) {
                binding.floatingActionButton.show();
            } else {
                binding.floatingActionButton.hide();
            }
        }
    };


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.app_menu_extention_search, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected: LocalSongs");
        if (item.getItemId() == R.id.search) {
            startActivity(new Intent(getContext(), SearchActivity.class));
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return super.onOptionsItemSelected(item);
    }

    public void onFabClicked(Playlists playlist) {
        binding.floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                CreatePlaylistBinding playlistBinding = CreatePlaylistBinding.inflate(getLayoutInflater());
                builder.setTitle("Create playlist")
                        .setView(playlistBinding.getRoot())
                        .setPositiveButton("SAVE", (dialog, which) -> {
                            String playlistName = String.valueOf(playlistBinding.playlistName.getText());
                            if (playlistBinding.supportYoutube.isChecked()) {
                                if (Repository.getInstance(context).createPlaylist(playlistName) == 0) {
                                    Toast.makeText(context, "Playlist Already Exist", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                ContentValues contentValues = new ContentValues();
                                contentValues.put(MediaStore.Audio.Playlists.NAME, playlistName);
                                contentValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
                                if (context.getContentResolver().insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, contentValues) == null)
                                    Toast.makeText(context, "Playlist Already Exist", Toast.LENGTH_SHORT).show();
                            }
                            playlist.onChanged();
                        })
                        .setNegativeButton("CANCEL", (dialog, which) -> {
                            dialog.dismiss();
                        });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }
}

