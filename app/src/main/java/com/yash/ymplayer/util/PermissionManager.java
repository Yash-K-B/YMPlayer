package com.yash.ymplayer.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.yash.ymplayer.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    public static List<String> requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return List.of(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return List.of(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return List.of(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO);
        } else {
            return List.of();
        }
    }

    public static boolean checkSelfPermission(Context context) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        List<String> permissions = requiredPermissions();
        for (String permission : permissions) {
            if(context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static void requestPermission(MainActivity activity) {
        requestPermission(activity, requiredPermissions());
    }

    public static void requestPermission(MainActivity activity, List<String> permissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return;
        activity.requestPermissions(permissions.toArray(new String[0]), 100);
    }
}
