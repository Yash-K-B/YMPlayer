package com.yash.ymplayer.download.manager;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import com.google.gson.Gson;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.R;
import com.yash.ymplayer.ffmpeg.AudioFile;
import com.yash.ymplayer.ffmpeg.AudioMetadata;
import com.yash.ymplayer.ffmpeg.FFMpegUtil;
import com.yash.ymplayer.models.DownloadFile;
import com.yash.ymplayer.util.DownloadUtil;
import com.yash.ymplayer.util.Keys;
import com.yash.youtube_extractor.Extractor;
import com.yash.youtube_extractor.exceptions.ExtractionException;
import com.yash.youtube_extractor.models.StreamingData;
import com.yash.youtube_extractor.models.VideoData;
import com.yash.youtube_extractor.models.VideoDetails;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class DownloadTask extends Thread {

    private static final String TAG = "DownloadTask";

    private Context context;
    private NotificationCompat.Builder notificationBuilder;
    private final String videoId;
    private DownloadService.Downloader downloader;
    private int bitrate = 128;
    private int taskId;
    private boolean isTerminate;
    private NotificationManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());

    SharedPreferences preferences;

    public DownloadTask(Context context, String videoId) {
        this.context = context;
        this.videoId = videoId;
        manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        preferences = context.getSharedPreferences(Keys.SHARED_PREFERENCES.DOWNLOADS, MODE_PRIVATE);

    }

    public DownloadTask(Context context, String videoId, int bitrate) {
        this.context = context;
        this.videoId = videoId;
        this.bitrate = bitrate;
        manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        preferences = context.getSharedPreferences(Keys.SHARED_PREFERENCES.DOWNLOADS, MODE_PRIVATE);
    }

    public DownloadTask(Context context, String videoId, int bitrate, int taskId) {
        this.context = context;
        this.videoId = videoId;
        this.bitrate = bitrate;
        this.taskId = taskId;
        manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        preferences = context.getSharedPreferences(Keys.SHARED_PREFERENCES.DOWNLOADS, MODE_PRIVATE);
    }

    void setCallbackObject(DownloadService.Downloader downloader) {
        this.downloader = downloader;
    }

    void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    String getVideoId() {
        return videoId;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void run() {
        super.run();
        LogHelper.d(TAG, "Start extracting audio with videoId : " + videoId);

        try {
            Extractor extractor = new Extractor();
            VideoDetails videoDetails = extractor.extract(videoId);

            List<StreamingData.AdaptiveAudioFormat> audioStreams = videoDetails.getStreamingData().getAdaptiveAudioFormats();
            LogHelper.d(TAG, "audio streams found: starting download --- ");

            Intent downloadFileIntent = new Intent(context, DownloadService.class);
            downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_ACTION, "pause");
            downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_VIDEO_ID, videoId);
            PendingIntent dFilePendingIntent;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                dFilePendingIntent = PendingIntent.getService(context, 10023, downloadFileIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            else
                dFilePendingIntent = PendingIntent.getService(context, 10023, downloadFileIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            notificationBuilder = new NotificationCompat.Builder(context, Keys.Notification.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_cloud_download)
                    .setColor(Color.BLUE)
                    .setContentTitle(videoDetails.getVideoData().getTitle())
                    .setContentText(videoDetails.getVideoData().getAuthor())
                    .setProgress(100, 0, false)
                    .addAction(R.drawable.icon_pause, "PAUSE", dFilePendingIntent);
            LogHelper.d(TAG, "onSuccess: " + videoDetails.getVideoData());


            try {
                File downloadFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), videoDetails.getVideoData().getVideoId() + ".aac");
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

                String file_name = videoDetails.getVideoData().getTitle();

                File directory;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    directory = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
                } else {
                    if (!Environment.getExternalStoragePublicDirectory("YMPlayer").exists()) // Deprecated
                        Environment.getExternalStoragePublicDirectory("YMPlayer").mkdirs();
                    directory = Environment.getExternalStoragePublicDirectory("YMPlayer");
                }

                File outFile = new File(directory, file_name + "-" + bitrate + "Kbps.mp3");
                int i = 0;
                while (outFile.exists()) {
                    i++;
                    outFile = new File(directory, file_name + "-" + i + "-" + bitrate + "Kbps.mp3");
                }

                VideoData.Thumbnail thumbnail = videoDetails.getVideoData().getThumbnail();
                File image = DownloadUtil.downloadTemp(thumbnail.getThumbnails().get(thumbnail.getThumbnails().size() - 1).getUrl());

                //"https://youtube.com/watch?v=" + videoId
                AudioMetadata audioMetadata = new AudioMetadata.Builder()
                        .withTitle(videoDetails.getVideoData().getTitle())
                        .withArtist(videoDetails.getVideoData().getAuthor())
                        .withAlbum("YMPlayer")
                        .withAlbumArt(image.getAbsolutePath())
                        .build();

                AudioFile audioFile = new AudioFile.Builder()
                        .audioMetadata(audioMetadata)
                        .inputFile(downloadFile.getAbsolutePath())
                        .outputFile(outFile.getPath())
                        .bitrate(bitrate + "k")
                        .sampleRate("48000")
                        .channelCount(2)
                        .build();

                FFMpegUtil.convert(audioFile, new FFMpegUtil.OnConversionListener() {
                    @Override
                    public void onSuccess(AudioFile audioFile) {

                    }

                    @Override
                    public void onFailed(AudioFile audioFile) {

                    }
                });

                LogHelper.d(TAG, "onSuccess: ");

                //Deleting temp files
                if (downloadFile.exists())
                    downloadFile.delete();

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    MediaScannerConnection.scanFile(context,
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

                    final ContentResolver resolver = context.getContentResolver();

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


                notificationBuilder.setContentText("Completed");
                Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                        .bigPicture(bitmap)
                        .setBigContentTitle(file_name));
                notificationBuilder.setLargeIcon(bitmap);
                notifyProgressComplete();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (ExtractionException e) {
            LogHelper.d(TAG, "ExtractionException : " + e.getLocalizedMessage());
            handler.post(() -> {
                Toast.makeText(context, "Error occurred while downloading", Toast.LENGTH_SHORT).show();
            });
        }

        downloader.taskFinished(videoId, taskId);

    }

    long prevTime = -1, currentTime;

    void notifyProgress(int progress) {
        if (isTerminate) return;
        currentTime = System.currentTimeMillis();
        if (prevTime == -1) prevTime = System.currentTimeMillis();
        else if (currentTime - prevTime < 250) return;
        prevTime = currentTime;
        notificationBuilder.setContentInfo(progress + "%");
        notificationBuilder.setProgress(100, progress, false);
        notificationBuilder.setOngoing(true);
        manager.notify(taskId, notificationBuilder.build());
        LogHelper.d(TAG, String.format(Locale.US, "progress : %d %%", progress));
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
        Intent downloadFileIntent = new Intent(context, DownloadService.class);
        downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_ACTION, "play");
        downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_VIDEO_ID, videoId);
        downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_TASK_ID, taskId);
        downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_BITRATE, bitrate);
        PendingIntent dFilePendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            dFilePendingIntent = PendingIntent.getService(context, 10023, downloadFileIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        else
            dFilePendingIntent = PendingIntent.getService(context, 10023, downloadFileIntent,  PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.mActions.set(0, new NotificationCompat.Action(R.drawable.icon_play, "Resume", dFilePendingIntent));
        notificationBuilder.setContentText("Paused");
        notificationBuilder.setOngoing(false);
        notificationBuilder.setContentInfo("");
        notificationBuilder.setSmallIcon(R.drawable.ic_pause_24);
        notificationBuilder.setProgress(0, 0, false);
        manager.notify(taskId, notificationBuilder.build());
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