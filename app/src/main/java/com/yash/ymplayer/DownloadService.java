package com.yash.ymplayer;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.yash.ymplayer.helper.LogHelper;
import com.yash.ymplayer.models.DownloadFile;
import com.yash.ymplayer.util.Keys;
import com.yash.youtube_extractor.Extractor;
import com.yash.youtube_extractor.models.StreamingData.AdaptiveAudioFormat;
import com.yash.youtube_extractor.models.VideoDetails;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    public static final int NOTIFICATION_REQ_ID = 1055;
    Downloader downloader;
    SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        downloader = new Downloader(2);
        preferences = this.getSharedPreferences(Keys.SHARED_PREFERENCES.DOWNLOADS,MODE_PRIVATE);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String videoId = intent.getStringExtra(Keys.VIDEO_ID);
        downloader.enqueue(new DownloaderThread(videoId));
        return super.onStartCommand(intent, flags, startId);
    }

    class Downloader {
        Queue<DownloaderThread> taskQueue;
        int reqId = 0;
        int runningTaskCount = 0;
        int maxRunningTasks;

        public Downloader(int maxRunningTasks) {
            this.maxRunningTasks = maxRunningTasks;
            taskQueue = new ArrayDeque<>();
        }

        void enqueue(DownloaderThread task) {
            LogHelper.d(TAG, "enqueue: ");
            task.setCallbackObject(this);
            task.setTaskId(reqId++);
            taskQueue.add(task);
            execute();
        }

        void execute() {
            LogHelper.d(TAG, "execute: ");
            if (runningTaskCount == maxRunningTasks) return;
            if (taskQueue.isEmpty()) return;
            DownloaderThread thread = taskQueue.remove();
            runningTaskCount++;
            LogHelper.d(TAG, "execute:  running task id: " + thread.getTaskId());
            thread.start();
        }

        void taskFinished(int id) {
            runningTaskCount--;
            LogHelper.d(TAG, "taskFinished id: " + id);
            if (!taskQueue.isEmpty()) execute();
            else if (runningTaskCount == 0) stopSelf();
        }
    }


    class DownloaderThread extends Thread {
        NotificationCompat.Builder notificationBuilder;
        String videoId;
        Downloader downloader;
        int taskId;
        NotificationManager manager;

        public DownloaderThread(String videoId) {
            this.videoId = videoId;
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        void setCallbackObject(Downloader downloader) {
            this.downloader = downloader;
        }

        void setTaskId(int taskId) {
            this.taskId = taskId;
        }

        int getTaskId() {
            return taskId;
        }

        @Override
        public void run() {
            super.run();
            LogHelper.d(TAG, "Start extracting audio with videoId : " + videoId);
            Extractor extractor = new Extractor();
            extractor.extract(videoId, new Extractor.Callback() {
                @Override
                public void onSuccess(VideoDetails videoDetails) {
                    List<AdaptiveAudioFormat> audioStreams = videoDetails.getStreamingData().getAdaptiveAudioFormats();
                    LogHelper.d(TAG, "audio streams found: starting download --- ");
                    byte[] bytes = new byte[1024];
                    notificationBuilder = new NotificationCompat.Builder(DownloadService.this, MainActivity.CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_download)
                            .setContentTitle(videoDetails.getVideoData().getTitle())
                            .setContentText(videoDetails.getVideoData().getAuthor())
                            .setProgress(100, 0, false);
                    LogHelper.d(TAG, "onSuccess: " + videoDetails.getVideoData().toString());

                    try {
                        File downloadFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), videoDetails.getVideoData().getTitle() + ".opus");
                        URL url = new URL(audioStreams.get(audioStreams.size() - 1).getUrl());
                        URLConnection connection = url.openConnection();
                        if(downloadFile.exists())
                            connection.setRequestProperty("Range", "bytes="+downloadFile.length()+"-");
                        int fileLength = connection.getContentLength() + (int) downloadFile.length();
                        /*InputStream stream = new BufferedInputStream(connection.getInputStream());

                        FileOutputStream outputStream = new FileOutputStream(downloadFile);
                        int count, total = (int)downloadFile.length();
                        while ((count = stream.read(bytes)) != -1) {
                            total += count;
                            outputStream.write(bytes, 0, count);
                            //LogHelper.d(TAG, "download progress: " + (total * 100 / fileLength) + "%");
                            notifyProgress((total * 100 / fileLength));

                        }
                        //LogHelper.d(TAG, "successfully downloaded!!!");
                        notifyProgressComplete();

                        outputStream.flush();
                        outputStream.close();
                        stream.close();*/
                        DownloadFile dFile = new DownloadFile();
                        dFile.fileImageUrl = videoDetails.getVideoData().getThumbnail().getThumbnails().get(1).getUrl();
                        dFile.fileLength = fileLength;
                        dFile.fileName = videoDetails.getVideoData().getTitle() + ".opus";
                        dFile.fileSubText = videoDetails.getVideoData().getAuthor();
                        Gson gson = new Gson();
                        int numDownloads = preferences.getInt(Keys.PREFERENCE_KEYS.TOTAL_DOWNLOADS,0);
                        String fileJson = gson.toJson(dFile);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(Keys.PREFERENCE_KEYS.DOWNLOADS+numDownloads,fileJson);
                        editor.putInt(Keys.PREFERENCE_KEYS.TOTAL_DOWNLOADS,numDownloads+1);
                        editor.apply();
                        downloader.taskFinished(taskId);
//                    FileInputStream inputStream = new FileInputStream(getExternalFilesDir(Environment.DIRECTORY_MUSIC)+videoData.getVideoDetails().getTitle()+"."+audioStreams.get(audioStreams.size()-1).getExtension());
//                    MediaExtractor extractor = new MediaExtractor();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            });

        }

        long prevTime = -1, currentTime;

        void notifyProgress(int progress) {
            currentTime = System.currentTimeMillis();
            if (prevTime == -1) prevTime = System.currentTimeMillis();
            else if (currentTime - prevTime < 1000) return;
            prevTime = currentTime;
            notificationBuilder.setProgress(100, progress, false);
            notificationBuilder.setOngoing(true);
            manager.notify(taskId, notificationBuilder.build());
        }

        void notifyProgressComplete() {
            prevTime = -1;
            notificationBuilder.setSmallIcon(R.drawable.ic_done_24);
            notificationBuilder.setProgress(0, 0, false);
            notificationBuilder.setOngoing(false);
            manager.notify(taskId, notificationBuilder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.d(TAG, "onDestroy: DownloadService");
    }
}
