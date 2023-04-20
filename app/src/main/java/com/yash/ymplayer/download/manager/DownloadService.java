package com.yash.ymplayer.download.manager;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.util.StringUtil;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    public static final int NOTIFICATION_REQ_ID = 1055;
    Downloader downloader;

    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.d(TAG, "onCreate: ");
        downloader = Downloader.getInstance(this, 2);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogHelper.d(TAG, "onStartCommand: %s", intent);
        if (intent == null) {
            LogHelper.d(TAG, "onStartCommand: Intent doesn't have task details");
            return START_NOT_STICKY;
        }
        String videoId = intent.getStringExtra(Keys.DownloadManager.EXTRA_VIDEO_ID);
        int downloadId = intent.getIntExtra(Keys.DownloadManager.DOWNLOAD_ID, 0);
        int bitrate = intent.getIntExtra(Keys.DownloadManager.EXTRA_BITRATE, 128);
        LogHelper.d(TAG, "onStartCommand: video id - %s, quality - %s, download id - %s", videoId, bitrate, downloadId);
        if (intent.hasExtra(Keys.DownloadManager.EXTRA_ACTION) && ("pause").equals(intent.getStringExtra(Keys.DownloadManager.EXTRA_ACTION))) {
            downloader.pause(downloadId, videoId);
        } else if (intent.hasExtra(Keys.DownloadManager.EXTRA_ACTION) && ("play").equals(intent.getStringExtra(Keys.DownloadManager.EXTRA_ACTION))) {
            downloader.resume(new DownloadTask(this, videoId, bitrate, downloadId));
        } else {
            if (!StringUtil.hasText(videoId))
                return super.onStartCommand(intent, flags, startId);

            downloader.enqueue(new DownloadTask(this, videoId, bitrate, downloadId));
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.d(TAG, "onDestroy: DownloadService");
    }
}
