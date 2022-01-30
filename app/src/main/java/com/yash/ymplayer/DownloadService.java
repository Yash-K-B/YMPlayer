package com.yash.ymplayer;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
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
import androidx.core.app.NotificationCompat;
import androidx.core.os.EnvironmentCompat;
import androidx.documentfile.provider.DocumentFile;

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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

public class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    public static final int NOTIFICATION_REQ_ID = 1055;
    Downloader downloader;
    SharedPreferences preferences;

    @Override
    public void onCreate() {
        super.onCreate();
        LogHelper.d(TAG, "onCreate: ");
        downloader = new Downloader(2);
        preferences = this.getSharedPreferences(Keys.SHARED_PREFERENCES.DOWNLOADS, MODE_PRIVATE);
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
            int taskId = intent.getIntExtra(Keys.DownloadManager.EXTRA_TASK_ID,100);
            int bitrate = intent.getIntExtra(Keys.DownloadManager.EXTRA_BITRATE,128);
            downloader.resume(new DownloaderThread(videoId,bitrate,taskId));
        } else {

            if (!intent.hasExtra(Keys.VIDEO_ID) || !intent.hasExtra(Keys.EXTRA_DOWNLOAD_QUALITY))
                return super.onStartCommand(intent, flags, startId);
            String videoId = intent.getStringExtra(Keys.VIDEO_ID);
            int bitrate = intent.getIntExtra(Keys.EXTRA_DOWNLOAD_QUALITY, 128);
            downloader.enqueue(new DownloaderThread(videoId, bitrate));
        }
        return START_NOT_STICKY;
    }

    class Downloader {
        Queue<DownloaderThread> taskQueue;
        LinkedList<DownloaderThread> runningTasks;
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

        void enqueue(DownloaderThread task) {
            LogHelper.d(TAG, "enqueue: ");
            Integer id = taskIds.poll();
            if (id == null) return;
            task.setCallbackObject(this);
            task.setTaskId(id);
            taskQueue.add(task);
            execute();
        }

        void resume(DownloaderThread thread){
            thread.setCallbackObject(this);
            taskQueue.add(thread);
            execute();
        }

        void execute() {
            LogHelper.d(TAG, "execute: ");
            if (runningTaskCount == maxRunningTasks) return;
            if (taskQueue.isEmpty()) return;
            DownloaderThread thread = taskQueue.poll();
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
                    DownloaderThread task = runningTasks.remove(i);
                    task.interrupt();
                    task.notifyProgressPaused();

                    LogHelper.d(TAG, "Task paused");
                    execute();

                }
            }
        }
    }


    class DownloaderThread extends Thread {
        NotificationCompat.Builder notificationBuilder;
        String videoId;
        Downloader downloader;
        int bitrate = 128;
        int taskId;
        boolean isTerminate;
        NotificationManager manager;
        Handler handler = new Handler(Looper.getMainLooper());

        public DownloaderThread(String videoId) {
            this.videoId = videoId;
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        }

        public DownloaderThread(String videoId, int bitrate) {
            this.videoId = videoId;
            this.bitrate = bitrate;
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        public DownloaderThread(String videoId, int bitrate,int taskId) {
            this.videoId = videoId;
            this.bitrate = bitrate;
            this.taskId = taskId;
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        void setCallbackObject(Downloader downloader) {
            this.downloader = downloader;
        }

        void setTaskId(int taskId) {
            this.taskId = taskId;
        }

        String getVideoId() {
            return videoId;
        }

        @Override
        public void run() {
            super.run();
            LogHelper.d(TAG, "Start extracting audio with videoId : " + videoId);

            try {
                Extractor extractor = new Extractor();
                VideoDetails videoDetails = extractor.extract(videoId);

                List<AdaptiveAudioFormat> audioStreams = videoDetails.getStreamingData().getAdaptiveAudioFormats();
                LogHelper.d(TAG, "audio streams found: starting download --- ");

                Intent downloadFileIntent = new Intent(DownloadService.this, DownloadService.class);
                downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_ACTION, "pause");
                downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_VIDEO_ID, videoId);
                PendingIntent dFilePendingIntent = PendingIntent.getService(DownloadService.this, 10023, downloadFileIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                notificationBuilder = new NotificationCompat.Builder(DownloadService.this, Keys.Notification.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_cloud_download)
                        .setColor(Color.BLUE)
                        .setContentTitle(videoDetails.getVideoData().getTitle())
                        .setContentText(videoDetails.getVideoData().getAuthor())
                        .setProgress(100, 0, false)
                        .addAction(R.drawable.icon_pause, "PAUSE", dFilePendingIntent);
                LogHelper.d(TAG, "onSuccess: " + videoDetails.getVideoData());


                try {
                    File downloadFile = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), videoDetails.getVideoData().getTitle() + ".aac");
                    URL url = new URL(audioStreams.get(0).getUrl());
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    if (downloadFile.exists())
                        connection.setRequestProperty("Range", "bytes=" + downloadFile.length() + "-");
                    int fileLength = connection.getContentLength() + (int) downloadFile.length();

                    if (connection.getResponseCode() != 416) {
                        byte[] bytes = new byte[4096];
                        InputStream stream = connection.getInputStream();
                        FileOutputStream outputStream = new FileOutputStream(downloadFile, true);
                        int count;
                        long total = downloadFile.length();
                        while ((count = stream.read(bytes)) != -1) {
                            total += count;
                            outputStream.write(bytes, 0, count);
                            notifyProgress((int) (((double) total / fileLength) * 100));

                        }
                        LogHelper.d(TAG, "successfully downloaded!!!");


                        outputStream.flush();
                        outputStream.close();
                        stream.close();
                    }

                    notificationBuilder.setContentText("Converting ...");
                    notifyIndeterminateProgress();
                    LogHelper.d(TAG, "Converting to MP3");

                    short[] buff = new short[65536];
                    byte[] mp3buff = new byte[65536];
                    String file_name = videoDetails.getVideoData().getTitle();
                    File r = new File(getFilesDir(), file_name + "-temp.mp3");

                    BufferedOutputStream fileWriter = new BufferedOutputStream(new FileOutputStream(r));
                    MediaExtractor mediaExtractor = new MediaExtractor();
                    mediaExtractor.setDataSource(downloadFile.getPath());
                    MediaFormat format = null;
                    if (mediaExtractor.getTrackCount() > 0) {
                        format = mediaExtractor.getTrackFormat(0);
                        mediaExtractor.selectTrack(0);
                    }

                    LogHelper.d(TAG, "onSuccess: " + format.toString());

                    int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

                    LameBuilder builder = new LameBuilder()
                            .setOutSampleRate(48000)   //48KHz
                            .setOutBitrate(bitrate)   //128 Kbps
                            .setOutChannels(channelCount);

                    AndroidLame lame = new AndroidLame(builder);

                    MediaCodec decoder = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
                    decoder.configure(format, null, null, 0);
                    decoder.start();
                    boolean isEnd = false;

                    while (!isEnd) {
                        int inputBufferId = decoder.dequeueInputBuffer(500);
                        if (inputBufferId >= 0) {
                            ByteBuffer buffer = decoder.getInputBuffer(inputBufferId);

                            int length = mediaExtractor.readSampleData(buffer, 0);
                            if (length > 0) {
                                decoder.queueInputBuffer(inputBufferId, 0, length, mediaExtractor.getSampleTime(), 0);
                                mediaExtractor.advance();
                            } else {
                                decoder.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                isEnd = true;
                            }
                        }

                        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                        int outputBufferId = decoder.dequeueOutputBuffer(info, 500);

                        if (outputBufferId >= 0) {
                            ByteBuffer outBuffer = decoder.getOutputBuffer(outputBufferId);
                            if (outBuffer == null) continue;
                            int length = info.size - info.offset;
                            byte[] bff = new byte[length];
                            outBuffer.get(bff);
                            int shortCount = ConverterUtil.bytesToShorts(bff, length, buff);
                            int bytesEncoded = lame.encodeBufferInterLeaved(buff, shortCount / 2, mp3buff);
                            try {
                                fileWriter.write(mp3buff, 0, bytesEncoded);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            decoder.releaseOutputBuffer(outputBufferId, info.presentationTimeUs);
                        } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            LogHelper.d(TAG, "onSuccess: INFO_OUTPUT_FORMAT_CHANGED");
                        }


                    }

                    decoder.stop();
                    decoder.release();
                    int count;
                    if ((count = lame.flush(mp3buff)) > 0) {
                        fileWriter.write(mp3buff, 0, count);
                    }
                    fileWriter.close();
                    mediaExtractor.release();

                    File directory;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        directory = getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                    } else {
                        if (!Environment.getExternalStoragePublicDirectory("YMPlayer").exists()) // Deprecated
                            Environment.getExternalStoragePublicDirectory("YMPlayer").mkdirs();
                        directory = Environment.getExternalStoragePublicDirectory("YMPlayer");
                    }


                    File outFile = new File(directory, file_name + "-"+bitrate+"Kbps.mp3");
                    int i = 0;
                    while (outFile.exists()) {
                        i++;
                        outFile = new File(directory, file_name + "-" + i + "-"+bitrate+"Kbps.mp3");
                    }

                    //Tags
                    Mp3File mp3File = new Mp3File(r.getAbsolutePath());
                    ID3v2 id3v2Tag = new ID3v24Tag();
                    mp3File.setId3v2Tag(id3v2Tag);
                    id3v2Tag.setArtist(videoDetails.getVideoData().getAuthor());
                    id3v2Tag.setTitle(videoDetails.getVideoData().getTitle());
                    id3v2Tag.setAlbum("YMPlayer");
                    id3v2Tag.setUrl("https://youtube.com/watch?v=" + videoId);
                    id3v2Tag.setEncoder("lame v3.100");

                    LogHelper.d(TAG, "onSuccess: ");
                    VideoData.Thumbnail thumbnail = videoDetails.getVideoData().getThumbnail();
                    byte[] image = DownloadUtil.download(thumbnail.getThumbnails().get(thumbnail.getThumbnails().size() - 2).getUrl());
                    id3v2Tag.setAlbumImage(image, "image/jpeg");

                    mp3File.save(outFile.getAbsolutePath());  // writing actual file

                    //Deleting temp files
                    if (downloadFile.exists()) downloadFile.delete();
                    if (r.exists()) r.delete();


                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        MediaScannerConnection.scanFile(DownloadService.this,
                                new String[]{outFile.getAbsolutePath()}, new String[]{MediaFormat.MIMETYPE_AUDIO_MPEG}, new MediaScannerConnection.MediaScannerConnectionClient() {
                                    @Override
                                    public void onMediaScannerConnected() {
                                        LogHelper.d(TAG, "onMediaScannerConnected: ");
                                    }

                                    @Override
                                    public void onScanCompleted(String path, Uri uri) {
                                        LogHelper.d(TAG, "onScanCompleted: ");
                                        addToDownloads(videoDetails, uri, fileLength, bitrate);
                                    }
                                });
                    } else {
                        final ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, videoDetails.getVideoData().getTitle());
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, MediaFormat.MIMETYPE_AUDIO_MPEG);
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MUSIC);

                        final ContentResolver resolver = getContentResolver();

                        OutputStream stream = null;
                        Uri uri = null;

                        try {
                            final Uri contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                            uri = resolver.insert(contentUri, contentValues);

                            if (uri == null) {
                                throw new IOException("Failed to create new MediaStore record.");
                            }

                            stream = resolver.openOutputStream(uri);

                            if (stream == null) {
                                throw new IOException("Failed to get output stream.");
                            }

                            byte[] bytes = new byte[4096];
                            int byteCount;
                            FileInputStream fin = new FileInputStream(outFile);
                            while ((byteCount = fin.read(bytes)) != -1) {
                                stream.write(bytes, 0, byteCount);
                            }
                            addToDownloads(videoDetails, uri, fileLength, bitrate);
                            if (outFile.exists())
                                outFile.delete();
                            fin.close();
                        } catch (IOException e) {
                            if (uri != null) {
                                // Don't leave an orphan entry in the MediaStore
                                resolver.delete(uri, null, null);
                                uri = null;
                            }

                            throw e;
                        } finally {
                            if (stream != null) {
                                stream.close();
                            }
                        }
                    }


                    //Refreshing MediaStore

/*

                        decoder.setCallback(new MediaCodec.Callback() {
                            @Override
                            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                                LogHelper.d(TAG, "onInputBufferAvailable: "+index);
                                ByteBuffer buffer = codec.getInputBuffer(index);

                                if (buffer == null) return;
                                int length = mediaExtractor.readSampleData(buffer, 0);
                                if (length > 0) {
                                    codec.queueInputBuffer(index, 0, length, mediaExtractor.getSampleTime(), 0);
                                    mediaExtractor.advance();
                                } else {
                                    codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                }

                            }

                            @Override
                            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                                LogHelper.d(TAG, "onOutputBufferAvailable: "+index);
                                ByteBuffer buffer = codec.getOutputBuffer(index);
                                if (buffer == null) return;
                                int count = info.size - info.offset;
                                if(count<1) return;
                                byte[] bs = new byte[count];
                                int position = buffer.position();
                                buffer.get(bs);
                                buffer.position(position);

                                int shortCount = ConverterUtil.bytesToShorts(bs, count, buff);
                                int bytesEncoded = lame.encodeBufferInterLeaved(buff, shortCount, mp3buff);
                                try {
                                    fileWriter.write(mp3buff, 0, bytesEncoded);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                codec.releaseOutputBuffer(index, info.presentationTimeUs);

                            }

                            @Override
                            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                                LogHelper.d(TAG, "onError: ");
                            }

                            @Override
                            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                                LogHelper.d(TAG, "onOutputFormatChanged: ");
                            }
                        });
                        decoder.configure(format, null, null, 0);
                        LogHelper.d(TAG, "onSuccess: Starting decoder");
                        decoder.start();

                        decoder.stop();
                        decoder.release();
*/


                    notificationBuilder.setContentText("Completed");
                    notificationBuilder.setLargeIcon(BitmapFactory.decodeByteArray(image, 0, image.length));
                    notifyProgressComplete();

                } catch (IOException | InvalidDataException | UnsupportedTagException | NotSupportedException e) {
                    e.printStackTrace();
                }

            } catch (ExtractionException e) {
                LogHelper.d(TAG, "ExtractionException : " + e.getLocalizedMessage());
                handler.post(() -> {
                    Toast.makeText(DownloadService.this, "Error occurred while downloading", Toast.LENGTH_SHORT).show();
                });
            }

            downloader.taskFinished(videoId, taskId);

        }

        long prevTime = -1, currentTime;

        void notifyProgress(int progress) {
            LogHelper.d(TAG, "notifyProgress: ");
            if (isTerminate) return;
            currentTime = System.currentTimeMillis();
            if (prevTime == -1) prevTime = System.currentTimeMillis();
            else if (currentTime - prevTime < 250) return;
            prevTime = currentTime;
            notificationBuilder.setContentInfo(progress + "%");
            notificationBuilder.setProgress(100, progress, false);
            notificationBuilder.setOngoing(true);
            manager.notify(taskId, notificationBuilder.build());
        }

        void notifyIndeterminateProgress() {
            LogHelper.d(TAG, "notifyIndeterminateProgress: ");
            if (isTerminate) return;
            notificationBuilder.setContentInfo("");
            notificationBuilder.setProgress(0, 0, true);
            notificationBuilder.setOngoing(true);
            manager.notify(taskId, notificationBuilder.build());
        }

        @SuppressLint("RestrictedApi")
        void notifyProgressComplete() {
            LogHelper.d(TAG, "notifyProgressComplete: ");
            prevTime = -1;
            notificationBuilder.mActions.clear();
            notificationBuilder.setContentInfo("");
            notificationBuilder.setSmallIcon(R.drawable.ic_done_24);
            notificationBuilder.setProgress(0, 0, false);
            notificationBuilder.setOngoing(false);
            manager.notify(taskId, notificationBuilder.build());
        }

        @SuppressLint("RestrictedApi")
        public void notifyProgressPaused() {
            LogHelper.d(TAG, "notifyProgressPaused: ");
            isTerminate = true;
            prevTime = -1;
            Intent downloadFileIntent = new Intent(DownloadService.this, DownloadService.class);
            downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_ACTION, "play");
            downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_VIDEO_ID, videoId);
            downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_TASK_ID, taskId);
            downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_BITRATE, bitrate);
            PendingIntent dFilePendingIntent = PendingIntent.getService(DownloadService.this, 10023, downloadFileIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder.mActions.set(0, new NotificationCompat.Action(R.drawable.icon_play, "Play", dFilePendingIntent));
            notificationBuilder.setContentText("Paused");
            notificationBuilder.setOngoing(false);
            notificationBuilder.setContentInfo("");
            notificationBuilder.setSmallIcon(R.drawable.ic_pause_24);
            notificationBuilder.setProgress(0, 0, false);
            manager.notify(taskId, notificationBuilder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogHelper.d(TAG, "onDestroy: DownloadService");
    }


    public void addToDownloads(VideoDetails videoDetails, Uri uri, long fileLength, int bitrate) {
        DownloadFile dFile = new DownloadFile();
        dFile.fileImageUrl = videoDetails.getVideoData().getThumbnail().getThumbnails().get(1).getUrl();
        dFile.fileLength = fileLength;
        dFile.fileName = videoDetails.getVideoData().getTitle();
        dFile.fileSubText = videoDetails.getVideoData().getAuthor();
        dFile.uri = uri.toString();
        dFile.bitrate = bitrate;
        dFile.extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(MediaFormat.MIMETYPE_AUDIO_MPEG);
        Gson gson = new Gson();
        int numDownloads = preferences.getInt(Keys.PREFERENCE_KEYS.TOTAL_DOWNLOADS, 0);
        String fileJson = gson.toJson(dFile);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(Keys.PREFERENCE_KEYS.DOWNLOADS + numDownloads, fileJson);
        editor.putInt(Keys.PREFERENCE_KEYS.TOTAL_DOWNLOADS, numDownloads + 1);
        editor.apply();
    }
}
