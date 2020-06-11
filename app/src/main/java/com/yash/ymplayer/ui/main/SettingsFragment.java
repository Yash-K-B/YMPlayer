package com.yash.ymplayer.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.yash.ymplayer.ActivityActionProvider;
import com.yash.ymplayer.BaseActivity;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.R;


public class SettingsFragment extends PreferenceFragmentCompat {
    private SharedPreferences preferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((ActivityActionProvider) getActivity()).setCustomToolbar(null, "Settings");
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.registerOnSharedPreferenceChangeListener(listener);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("theme") || key.equals("background")) {
                ((MainActivity)getActivity()).refresh();
            }
            if(key.equals("user_name")){
                ((ActivityActionProvider) getActivity()).setUserName(sharedPreferences.getString("user_name","User@YMPlayer"));
            }
        }
    };
}
