package com.yash.ymplayer.ui.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.yash.ymplayer.ActivityActionProvider;
import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.PlayerService;
import com.yash.ymplayer.R;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.equaliser.Settings;
import com.yash.ymplayer.util.EqualizerUtil;
import com.yash.ymplayer.util.Keys;

import java.security.Key;
import java.util.Objects;


public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String TAG = "SettingsFragment";
    private SharedPreferences preferences;
    private Context context;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ActivityActionProvider) context).setCustomToolbar(null, "Settings");
        preferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case "theme":
                case "background":
                    ((MainActivity) context).refresh();
                    break;

                case "user_name":
                    ((ActivityActionProvider) context).setUserName(sharedPreferences.getString("user_name", "User@YMPlayer"));
                    break;

                case Keys.PREFERENCE_KEYS.BUILTIN_EQUALIZER:
                    boolean equalizerState = sharedPreferences.getBoolean(Keys.PREFERENCE_KEYS.BUILTIN_EQUALIZER, false);
                    EqualizerUtil.getInstance(context).setEqualizerEnabled(equalizerState);
                    break;

                case Keys.PREFERENCE_KEYS.PLAYBACK_QUALITY:
                    ((ActivityActionProvider) context).sendActionToMediaSession(Keys.Action.PLAYBACK_QUALITY_CHANGED, null);
                    break;

                case Keys.PREFERENCE_KEYS.LOUDNESS_GAIN:
                    try {
                        int gain = Integer.parseInt(sharedPreferences.getString(Keys.PREFERENCE_KEYS.LOUDNESS_GAIN, "1000"));
                        EqualizerUtil.getInstance(context).reloadLoudnessGain(gain);
                    } catch (NumberFormatException e) {
                        LogHelper.d(TAG, e.getMessage());
                    }


                    break;

                default:
                    LogHelper.d(TAG, "No Action Preference Key. ");
            }


        }
    };
}
