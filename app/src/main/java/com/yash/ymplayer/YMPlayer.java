package com.yash.ymplayer;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.util.ConverterUtil;
import com.yash.ymplayer.util.Keys;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YMPlayer extends Application {
    private static final String TAG = "YMPlayer";
    SharedPreferences preferences;
    UncaughtExceptionHandler defaultUncaughtExceptionHandler;
    ExecutorService executor;

    public YMPlayer(){
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences  = PreferenceManager.getDefaultSharedPreferences(this);
        LogHelper.deploy(this, TAG);
        executor = Executors.newSingleThreadExecutor();


        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {

            String exception = ConverterUtil.toStringStackTrace(e);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Keys.PREFERENCE_KEYS.IS_EXCEPTION,true);
            editor.putString(Keys.PREFERENCE_KEYS.EXCEPTION,exception);
            editor.apply();

            defaultUncaughtExceptionHandler.uncaughtException(t,e);
        });
    }


    @Override
    public void onTerminate() {
        super.onTerminate();
        LogHelper.flush();

    }


    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LogHelper.flush();

    }
}
