package com.yash.ymplayer;

import android.content.ComponentName;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.annotation.Nullable;

import com.yash.ymplayer.databinding.FragmentMainBinding;
import com.yash.ymplayer.equaliser.EqualizerFragment;
import com.yash.ymplayer.util.Keys;

public class EqualizerActivity extends BaseActivity {
    private static final String TAG = "EqualiserActivity";
    FragmentMainBinding layoutBinding;
    MediaBrowserCompat mediaBrowser;
    MediaControllerCompat mediaController;
    Handler handler;

    EqualizerFragment equalizerFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        layoutBinding = FragmentMainBinding.inflate(getLayoutInflater());
        setContentView(layoutBinding.getRoot());
        handler = new Handler(Looper.getMainLooper());

        equalizerFragment = EqualizerFragment.newBuilder()
                .setAccentColor(BaseActivity.getAttributeColor(EqualizerActivity.this,R.attr.colorAccent))    //Color.parseColor("#4caf50")
                .setAudioSessionId(0)
                .build();
        getSupportFragmentManager().beginTransaction()
                .replace(layoutBinding.container.getId(), equalizerFragment)
                .commit();
    }

    @Override
    public void refresh() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
