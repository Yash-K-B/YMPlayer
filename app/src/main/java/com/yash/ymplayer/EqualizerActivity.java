package com.yash.ymplayer;

import android.content.ComponentName;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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
        handler = new Handler();
        mediaBrowser = new MediaBrowserCompat(this, new ComponentName(this, PlayerService.class), connectionCallback, null);
        mediaBrowser.connect();
    }

    @Override
    public void refresh() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaController.getTransportControls().sendCustomAction(Keys.COMMAND.UPDATE_EQUALIZER, null);
        mediaBrowser.disconnect();
    }

    MediaBrowserCompat.ConnectionCallback connectionCallback = new MediaBrowserCompat.ConnectionCallback() {
        @Override
        public void onConnected() {
            try {

                mediaController = new MediaControllerCompat(EqualizerActivity.this, mediaBrowser.getSessionToken());
                mediaController.sendCommand(Keys.COMMAND.ON_AUDIO_SESSION_ID_CHANGE,null,new ResultReceiver(handler){
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if(equalizerFragment != null)
                            equalizerFragment.updateAudioSessionId(resultData.getInt(Keys.AUDIO_SESSION_ID));
                    }
                });
                mediaController.sendCommand(Keys.COMMAND.GET_AUDIO_SESSION_ID, null, new ResultReceiver(handler) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if (resultCode == 1001) {
                            equalizerFragment = EqualizerFragment.newBuilder()
                                    .setAccentColor(BaseActivity.getAttributeColor(EqualizerActivity.this,R.attr.colorAccent))    //Color.parseColor("#4caf50")
                                    .setAudioSessionId(resultData.getInt(Keys.AUDIO_SESSION_ID))
                                    .build();
                            getSupportFragmentManager().beginTransaction()
                                    .replace(layoutBinding.container.getId(), equalizerFragment)
                                    .commit();
                        }

                    }
                });

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

}
