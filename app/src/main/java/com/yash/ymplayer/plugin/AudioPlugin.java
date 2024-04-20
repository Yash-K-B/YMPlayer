package com.yash.ymplayer.plugin;

import android.content.Context;

public class AudioPlugin {
    private static AudioPlugin instance;
    private AudioPlugin(Context context) {

    }
    public static AudioPlugin getInstance(Context context) {
        if (instance == null) {
            instance = new AudioPlugin(context.getApplicationContext());
        }
        return instance;
    }


    public void initializePlugin() {
        // download ffmpeg


    }





}
