package com.yash.ymplayer.ui.main;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayoutMediator;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.interfaces.ActivityActionProvider;
import com.yash.ymplayer.R;
import com.yash.ymplayer.SearchActivity;
import com.yash.ymplayer.databinding.CreatePlaylistBinding;
import com.yash.ymplayer.databinding.FragmentLocalSongsBinding;
import com.yash.ymplayer.interfaces.EmbeddedListener;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.ui.custom.ConnectionAwareFragment;
import com.yash.ymplayer.util.ConverterUtil;

/**
 * A simple {@link Fragment} subclass.
 */
public class LocalSongs extends ConnectionAwareFragment {
    private static final String[] TAB_TITLES = new String[]{"All Songs", "Albums", "Artists", "Playlist"};
    private static final String TAG = "LocalSongs";
    FragmentLocalSongsBinding binding;
    Context context;
    FragmentActivity activity;

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
        binding.floatingActionButton.hide();
    }

    private void initFabAnchor() {
        View anchorPlayerView = activity.findViewById(R.id.playerGuide);
        adjustFabPosition(anchorPlayerView, binding.floatingActionButton);
    }

    private void adjustFabPosition(View playerView, FloatingActionButton fab) {
        if (playerView != null && fab != null) {
            playerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {

                    playerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                    // Get playerView's position on the screen
                    int[] playerViewLocation = new int[2];
                    playerView.getLocationOnScreen(playerViewLocation);

                    // Adjust FAB position relative to playerView
                    int xPosition = (int) (/*playerViewLocation[0] */ playerView.getWidth() - fab.getWidth() - ConverterUtil.getPx(playerView.getContext(), 16));
                    int yPosition = (int) (playerViewLocation[1] - fab.getHeight() - ConverterUtil.getPx(playerView.getContext(), 40));

                    fab.setX(xPosition);
                    fab.setY(yPosition);
                }
            });
        }
    }


    @Override
    public void onConnected(MediaControllerCompat mediaController) {
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(getChildFragmentManager(), activity.getLifecycle());
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            binding.viewPager.setAdapter(sectionsPagerAdapter);
            new TabLayoutMediator(binding.tabs, binding.viewPager, (tab, position) -> tab.setText(TAB_TITLES[position])).attach();
            binding.viewPager.registerOnPageChangeCallback(viewPagerPageChangeCallback);
            binding.viewPager.setOffscreenPageLimit(1);
            binding.progressBar.setVisibility(View.GONE);
        }, 200);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding.viewPager.unregisterOnPageChangeCallback(viewPagerPageChangeCallback);
    }

    private void adjustFabAndShow() {
        initFabAnchor();
        binding.floatingActionButton.show();
    }

    private final ViewPager2.OnPageChangeCallback viewPagerPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            ((EmbeddedListener)activity).onPageChange();
            if (position == 3) {
                adjustFabAndShow();
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
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

