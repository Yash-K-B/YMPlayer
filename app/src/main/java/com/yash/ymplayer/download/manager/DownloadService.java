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
import com.yash.ymplayer.util.StringUtil;
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
