package com.yash.ymplayer.download.manager;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;
import com.naman14.androidlame.AndroidLame;
import com.naman14.androidlame.LameBuilder;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.R;
import com.yash.ymplayer.models.DownloadFile;
import com.yash.ymplayer.util.ConverterUtil;
import com.yash.ymplayer.util.DownloadUtil;
import com.yash.ymplayer.util.Keys;
import com.yash.youtube_extractor.Extractor;
import com.yash.youtube_extractor.exceptions.ExtractionException;
import com.yash.youtube_extractor.models.StreamingData.AdaptiveAudioFormat;
import com.yash.youtube_extractor.models.VideoData;
import com.yash.youtube_extractor.models.VideoDetails;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    public static final int NOTIFICATION_REQ_ID = 1055;
    Downloader downloader;

    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.d(TAG, "onCreate: ");
        downloader = new Downloader(2);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogHelper.d(TAG, "onStartCommand: ");
        if (intent == null) return START_NOT_STICKY;
        if (intent.hasExtra(Keys.DownloadManager.EXTRA_ACTION) && ("pause").equals(intent.getStringExtra(Keys.DownloadManager.EXTRA_ACTION))) {
            downloader.pause(intent.getStringExtra(Keys.DownloadManager.EXTRA_VIDEO_ID));
        } else if (intent.hasExtra(Keys.DownloadManager.EXTRA_ACTION) && ("play").equals(intent.getStringExtra(Keys.DownloadManager.EXTRA_ACTION))) {
            String videoId = intent.getStringExtra(Keys.DownloadManager.EXTRA_VIDEO_ID);
            int taskId = intent.getIntExtra(Keys.DownloadManager.EXTRA_TASK_ID, 100);
            int bitrate = intent.getIntExtra(Keys.DownloadManager.EXTRA_BITRATE, 128);
            downloader.resume(new DownloadTask(this, videoId, bitrate, taskId));
        } else {

            if (!intent.hasExtra(Keys.VIDEO_ID) || !intent.hasExtra(Keys.EXTRA_DOWNLOAD_QUALITY))
                return super.onStartCommand(intent, flags, startId);
            String videoId = intent.getStringExtra(Keys.VIDEO_ID);
            int bitrate = intent.getIntExtra(Keys.EXTRA_DOWNLOAD_QUALITY, 128);
            downloader.enqueue(new DownloadTask(this, videoId, bitrate));
        }
        return START_NOT_STICKY;
    }

    class Downloader {
        Queue<DownloadTask> taskQueue;
        LinkedList<DownloadTask> runningTasks;
        int runningTaskCount = 0;
        int maxRunningTasks;
        Queue<Integer> taskIds;

        public Downloader(int maxRunningTasks) {
            this.maxRunningTasks = maxRunningTasks;
            taskQueue = new ArrayDeque<>();
            runningTasks = new LinkedList<>();
            taskIds = new ArrayDeque<>(100);
            for (int i = 100; i < 200; i++)
                taskIds.add(i);
        }

        void enqueue(DownloadTask task) {
            LogHelper.d(TAG, "enqueue: ");
            Integer id = taskIds.poll();
            if (id == null) return;
            task.setCallbackObject(this);
            task.setTaskId(id);
            taskQueue.add(task);
            execute();
        }

        void resume(DownloadTask thread) {
            thread.setCallbackObject(this);
            taskQueue.add(thread);
            execute();
        }

        void execute() {
            LogHelper.d(TAG, "execute: ");
            if (runningTaskCount == maxRunningTasks) return;
            if (taskQueue.isEmpty()) return;
            DownloadTask thread = taskQueue.poll();
            runningTasks.add(thread);
            runningTaskCount++;
            LogHelper.d(TAG, "execute:  running task videoId: " + thread.getVideoId());
            thread.start();
        }

        void taskFinished(String videoId, int taskId) {
            taskIds.add(taskId);
            runningTaskCount--;
            LogHelper.d(TAG, "taskFinished videoId: " + videoId);
            if (!taskQueue.isEmpty()) execute();
            else if (runningTaskCount == 0) DownloadService.this.stopSelf();
        }

        public void pause(String videoId) {
            for (int i = 0; i < runningTasks.size(); i++) {
                if (runningTasks.get(i).getVideoId().equals(videoId)) {
                    DownloadTask task = runningTasks.remove(i);
                    task.interrupt();
                    task.notifyProgressPaused();

                    LogHelper.d(TAG, "Task paused");
                    execute();

                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.d(TAG, "onDestroy: DownloadService");
    }
}
