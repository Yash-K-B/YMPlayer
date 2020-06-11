package com.yash.ymplayer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public abstract class BaseActivity extends AppCompatActivity {
    private static final String TAG = "debug";
    SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String theme_name = preferences.getString("theme", "blue") + preferences.getString("background", "light");
        Log.d(TAG, "onCreate: theme:" + theme_name);
        switch (theme_name) {
            case "bluedark":
                setTheme(R.style.YTheme_Dark_Blue_NoActionBar);
                break;
            case "greendark":
                setTheme(R.style.YTheme_Dark_Green_NoActionBar);
                break;
            case "reddark":
                setTheme(R.style.YTheme_Dark_Red_NoActionBar);
                break;
            case "bluelight":
                setTheme(R.style.YTheme_Light_Blue_NoActionBar);
                break;
            case "greenlight":
                setTheme(R.style.YTheme_Light_Green_NoActionBar);
                break;
            case "redlight":
                setTheme(R.style.YTheme_Light_Red_NoActionBar);
                break;
            case "blueamoled":
                setTheme(R.style.YTheme_AMOLED_Blue_NoActionBar);
                break;
            case "greenamoled":
                setTheme(R.style.YTheme_AMOLED_Green_NoActionBar);
                break;
            case "redamoled":
                setTheme(R.style.YTheme_AMOLED_Red_NoActionBar);
                break;
        }
        super.onCreate(savedInstanceState);
    }

    /**
     *
     */
    abstract public void refresh();


}
