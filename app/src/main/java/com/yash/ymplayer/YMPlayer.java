package com.yash.ymplayer;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.util.ConverterUtil;
import com.yash.ymplayer.util.Keys;

import java.lang.Thread.UncaughtExceptionHandler;

public class YMPlayer extends Application {
    private static final String TAG = "YMPlayer";
    SharedPreferences preferences;
    UncaughtExceptionHandler defaultUncaughtExceptionHandler;

    public YMPlayer(){
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        preferences  = PreferenceManager.getDefaultSharedPreferences(this);
        LogHelper.deploy(this, TAG);


        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {

                String exception = e.toString() + "\n\n" + ConverterUtil.toStringStackTrace(e.getStackTrace());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(Keys.PREFERENCE_KEYS.IS_EXCEPTION,true);
                editor.putString(Keys.PREFERENCE_KEYS.EXCEPTION,exception);
                editor.commit();
                //System.exit(2);
                defaultUncaughtExceptionHandler.uncaughtException(t,e);
            }
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
