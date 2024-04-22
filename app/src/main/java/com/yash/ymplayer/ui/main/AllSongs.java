package com.yash.ymplayer.ui.main;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentAllSongsBinding;
import com.yash.ymplayer.ui.custom.ConnectionAwareFragment;
import com.yash.ymplayer.util.CommonUtil;
import com.yash.ymplayer.util.SongsContextMenuClickListener;
import com.yash.ymplayer.util.SongsAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class AllSongs extends Fragment {
    private static final String TAG = "AllSongs";
    Context context;
    FragmentActivity activity;
    LocalViewModel viewModel;
    FragmentAllSongsBinding allSongsBinding;
    SongsAdapter songsAdapter;

    public AllSongs() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        allSongsBinding = FragmentAllSongsBinding.inflate(inflater, container, false);
        return allSongsBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = requireContext();
        activity = requireActivity();
        viewModel = new ViewModelProvider(activity).get(LocalViewModel.class);
        allSongsBinding.listRv.setLayoutManager(new LinearLayoutManager(context));
        allSongsBinding.listRv.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
        initUi();
    }


    public void initUi() {
        ConnectionAwareFragment connectionAwareFragment = CommonUtil.getConnectionAwareFragment(this);
        MediaControllerCompat mediaController = connectionAwareFragment.getMediaController();
        songsAdapter = new SongsAdapter(context,launcher, (v, song) -> {
            mediaController.getTransportControls().playFromMediaId(song.getDescription().getMediaId(), null);
            Log.d(TAG, "onClick: Extra: null");
        }, new SongsContextMenuClickListener(context, mediaController), SongsAdapter.MODE.ALL);
        allSongsBinding.listRv.setAdapter(songsAdapter);
        allSongsBinding.allSongsRefresh.setColorSchemeColors(BaseActivity.getAttributeColor(context, R.attr.colorPrimary));
        allSongsBinding.allSongsRefresh.setOnRefreshListener(() -> {
            allSongsBinding.allSongsRefresh.setRefreshing(true);
            viewModel.refresh(getContext(), connectionAwareFragment.getMediaBrowser());
            LogHelper.d(TAG, "onConnected: Refresh completed");
        });
        if (viewModel.songs.getValue() == null || viewModel.songs.getValue().isEmpty())
            viewModel.loadSongs(connectionAwareFragment.getMediaBrowser());
        viewModel.songs.observe(getViewLifecycleOwner(), songs -> {
            Log.d(TAG, "onChanged: Song Refreshed");
            allSongsBinding.allSongsRefresh.setRefreshing(false);
            allSongsBinding.progressBar.setVisibility(View.INVISIBLE);
            songsAdapter.refreshList(songs);
        });
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }


    private final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show();
                }
            });


}
