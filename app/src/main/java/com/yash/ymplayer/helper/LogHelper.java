package com.yash.ymplayer.helper;

import android.util.Log;

public class LogHelper {
    private static final String ACTUAL_TAG = "YMPLAYER_LOGS";

    public static void v(String TAG, String message) {
        Log.v(ACTUAL_TAG, TAG + " : " + message);
    }

    public static void d(String TAG, String message) {
        Log.d(ACTUAL_TAG, TAG + " : " + message);
    }

    public static void i(String TAG, String message) {
        Log.i(ACTUAL_TAG, TAG + " : " + message);
    }

    public static void w(String TAG, String message) {
        Log.w(ACTUAL_TAG, TAG + " : " + message);
    }

    public static void e(String TAG, String message) {
        Log.e(ACTUAL_TAG, TAG + " : " + message);
    }
}
