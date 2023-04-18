package com.yash.ymplayer.download.manager;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;

import android.annotation.SuppressLint;
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
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.arthenica.ffmpegkit.FFmpegSession;
import com.google.common.net.HttpHeaders;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.R;
import com.yash.ymplayer.download.manager.constants.DownloadStatus;
import com.yash.ymplayer.download.manager.dao.DownloadDao;
import com.yash.ymplayer.download.manager.models.Download;
import com.yash.ymplayer.ffmpeg.AudioFile;
import com.yash.ymplayer.ffmpeg.AudioMetadata;
import com.yash.ymplayer.ffmpeg.FFMpegUtil;
import com.yash.ymplayer.util.DownloadUtil;
import com.yash.ymplayer.util.Keys;
import com.yash.youtube_extractor.Extractor;
import com.yash.youtube_extractor.exceptions.ExtractionException;
import com.yash.youtube_extractor.models.StreamingData;
import com.yash.youtube_extractor.models.VideoData;
import com.yash.youtube_extractor.models.VideoDetails;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends Thread {

    public final int NUM_THREADS;

    private static final String TAG = "DownloadTask";

    private final Context context;
    private NotificationCompat.Builder notificationBuilder;
    private final String videoId;
    private Downloader downloader;
    private int bitrate = 128;
    private int taskId;
    private boolean isTerminate;
    private final NotificationManager manager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executorService;
    private boolean isCanceled;
    private int downloadId;

    SharedPreferences preferences;

    FFmpegSession fFmpegSession = null;
    MutableLiveData<DownloadStatus> downloadStatusLiveData = new MutableLiveData<>();
    MutableLiveData<DownloadProgress> downloadProgressLiveData = new MutableLiveData<>();
    private static final int BUFFER_SIZE = 32 * 1024; // 32 KB

    public DownloadTask(Context context, String videoId) {
        this(context, videoId, 128, 0);
    }

    public DownloadTask(Context context, String videoId, int bitrate) {
        this(context, videoId, bitrate, 0);
    }

    public DownloadTask(Context context, String videoId, int bitrate, int downloadId) {
        this.context = context;
        this.videoId = videoId;
        this.bitrate = bitrate;
        this.downloadId = downloadId;
        manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        NUM_THREADS = Integer.parseInt(preferences.getString("download_threads", "4"));
        executorService = Executors.newFixedThreadPool(NUM_THREADS);
    }

    void setCallbackObject(Downloader downloader) {
        this.downloader = downloader;
    }

    void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getVideoId() {
        return videoId;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getDownloadId() {
        return downloadId;
    }

    public MutableLiveData<DownloadStatus> getStatus() {
        return downloadStatusLiveData;
    }

    public MutableLiveData<DownloadProgress> getProgress() {
        return downloadProgressLiveData;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void run() {
        super.run();
        LogHelper.d(TAG, "Start extracting audio with videoId : " + videoId);

        try {
            Extractor extractor = new Extractor();
            VideoDetails videoDetails = extractor.extract(videoId);

            if (downloadId == 0) {
                LogHelper.d(TAG, "run: Creating entry in the downloads [bitrate - %s]", bitrate);
                downloadId = addToDownloads(videoDetails, bitrate);
            } else {
                LogHelper.d(TAG, "run: Has download id - %s", downloadId);
            }

            updateStatus(DownloadStatus.DOWNLOADING);
            List<StreamingData.AdaptiveAudioFormat> audioStreams = videoDetails.getStreamingData().getAdaptiveAudioFormats();
            LogHelper.d(TAG, "audio streams found: starting download --- ");

            PendingIntent dFilePendingIntent = getPendingIntentByAction(Action.PAUSE);
            notificationBuilder = new NotificationCompat.Builder(context, Keys.Notification.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_cloud_download)
                    .setColor(Color.BLUE)
                    .setContentTitle(videoDetails.getVideoData().getTitle())
                    .setContentText(videoDetails.getVideoData().getAuthor())
                    .setProgress(100, 0, false)
                    .addAction(R.drawable.icon_pause, "PAUSE", dFilePendingIntent);
            LogHelper.d(TAG, "onSuccess: " + videoDetails.getVideoData());


            int index = getIndex(bitrate, audioStreams);
            String url = audioStreams.get(index).getUrl();
            LogHelper.d(TAG, "Downloading file using url : %s", url);
            if(!url.contains("ratebypass"))
                url = url + "&ratebypass=yes";
            Uri httpUri = Uri.parse(url);
            String format = httpUri.getQueryParameter("mime") != null? httpUri.getQueryParameter("mime").split("/")[1]: "aac";
            int fileLength = Integer.parseInt(httpUri.getQueryParameter("clen"));
            DownloadRepository.getInstance(context).getDownloadDao().updateLength(downloadId, fileLength);
            String fileName = String.format(Locale.US, "%s-%d.%s", videoDetails.getVideoData().getVideoId(), index, format);
            File downloadFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), fileName);
            updateProgress(0, fileLength);
            if (downloadFile.length() == fileLength) {
                LogHelper.d(TAG, "run: File already downloaded and merged");
            } else {
                LogHelper.d(TAG, "Starting parallel download with threads %s",  NUM_THREADS);
                downloadWithParallel(url, fileLength, bitrate, downloadFile);
            }

            if (DownloadTask.this.isCanceled) {
                updateStatus(DownloadStatus.PAUSED);
                LogHelper.d(TAG, "Download with id %s has been canceled by the user", videoId);
                return;
            }

            updateProgress(fileLength, fileLength);
            updateStatus(DownloadStatus.PROCESSING);
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

            File outFile = new File(directory, file_name + "-" + bitrate + " Kbps.mp3");
            int i = 0;
            while (outFile.exists()) {
                i++;
                outFile = new File(directory, file_name + "-" + i + "-" + bitrate + " Kbps.mp3");
            }

            VideoData.Thumbnail thumbnail = videoDetails.getVideoData().getThumbnail();
            File image = DownloadUtil.downloadTemp(thumbnail.getThumbnails().get(thumbnail.getThumbnails().size() - 1).getUrl());

            //"https://youtube.com/watch?v=" + videoId
            AudioMetadata audioMetadata = new AudioMetadata.Builder()
                    .withTitle(videoDetails.getVideoData().getTitle())
                    .withArtist(videoDetails.getVideoData().getAuthor())
                    .withAlbum("YMPlayer")
                    .withAlbumArt(image.getPath())
                    .build();

            AudioFile audioFile = new AudioFile.Builder()
                    .audioMetadata(audioMetadata)
                    .inputFile(downloadFile.getAbsolutePath())
                    .outputFile(outFile.getPath())
                    .bitrate(bitrate + "k")
                    .channelCount(2)
                    .build();

            if (DownloadTask.this.isCanceled) {
                updateStatus(DownloadStatus.PAUSED);
                LogHelper.d(TAG, "Download with id %s has been canceled by the user", videoId);
                return;
            }

            fFmpegSession = FFMpegUtil.createSession(audioFile, new FFMpegUtil.OnConversionListener() {
                @Override
                public void onSuccess(AudioFile audioFile) {

                }

                @Override
                public void onFailed(AudioFile audioFile) {

                }
            });

            FFMpegUtil.convert(fFmpegSession);

            if (DownloadTask.this.isCanceled) {
                updateStatus(DownloadStatus.PAUSED);
                LogHelper.d(TAG, "Download with id %s has been canceled by the user", videoId);
                return;
            }


            LogHelper.d(TAG, "File downloaded and converted successfully");


            //Deleting temp files
//            if (downloadFile.exists())
//                downloadFile.delete();

            DownloadRepository.getInstance(context).getDownloadDao().updateLength(downloadId, outFile.length());

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
                                DownloadRepository.getInstance(context).getDownloadDao().updateUri(downloadId, uri.toString());
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
                    DownloadRepository.getInstance(context).getDownloadDao().updateUri(downloadId, uri.toString());
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

            updateStatus(DownloadStatus.DOWNLOADED);

            notificationBuilder.setContentText("Completed");
            Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .setBigContentTitle(file_name));
            notificationBuilder.setLargeIcon(bitmap);
            notifyProgressComplete();

        } catch (Exception e) {
            LogHelper.e(TAG, "Exception while downloading mp3", e);
            notifyProgressPaused(isCanceled? "Paused" : "Failed");
            updateStatus(isCanceled? DownloadStatus.PAUSED: DownloadStatus.FAILED);
            if (!isCanceled)
                handler.post(() -> {
                    Toast.makeText(context, "Error occurred while downloading", Toast.LENGTH_SHORT).show();
                });
        }

        executorService.shutdown();
        downloader.taskFinished(this);

    }

    private PendingIntent getPendingIntentByAction(Action action) {
        PendingIntent dFilePendingIntent;
        Intent downloadFileIntent = new Intent(context, DownloadService.class);
        downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_ACTION, action == Action.RESUME ? "play" : "pause");
        downloadFileIntent.putExtra(Keys.DownloadManager.EXTRA_VIDEO_ID, videoId);
        downloadFileIntent.putExtra(Keys.DownloadManager.DOWNLOAD_ID, downloadId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            dFilePendingIntent = PendingIntent.getService(context, 10023, downloadFileIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        else
            dFilePendingIntent = PendingIntent.getService(context, 10023, downloadFileIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return dFilePendingIntent;
    }

    @SuppressLint("CheckResult")
    private void downloadWithParallel(String url, int fileLength, int bitrate, File file) throws IOException, InterruptedException {
        if (fileLength == 0)
            return;

        String partNameFormat = "%s.part.%s.%s-%s";

        long blockSize = fileLength / NUM_THREADS; // Divide the file size into NUM_THREADS blocks

        AtomicLong bytesReceived = new AtomicLong(0);

        String fileName = file.getName();

        List<Single<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < NUM_THREADS; i++) {
            final long[] startByte = {i * blockSize};
            long endByte = (i + 1) * blockSize - 1;
            if (i == NUM_THREADS - 1) {
                // The last thread downloads the remaining bytes
                endByte = fileLength - 1;
            }
            long finalEndByte = endByte;
            int finalI = i;
            Single<Boolean> uCompletableFuture = Single.fromSupplier(() -> {
                try {
                    String partFileName = String.format(partNameFormat, fileName, bitrate, NUM_THREADS, finalI);
                    File downloadFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), partFileName);
                    URL httpUrl = new URL(url);
                    HttpsURLConnection connection = (HttpsURLConnection) httpUrl.openConnection();
                    if (downloadFile.exists())
                        startByte[0] = startByte[0] + downloadFile.length();

                    connection.setRequestProperty("Range", "bytes=" + startByte[0] + "-" + finalEndByte);
                    connection.setRequestProperty("accept-encoding", "gzip, deflate, br");
                    connection.setRequestProperty("accept-language", "en-GB,en-US;q=0.9,en;q=0.8");
                    connection.setRequestProperty("sec-fetch-mode", "navigate");
                    connection.setRequestProperty("sec-fetch-site", "same-origin");
                    connection.setRequestProperty("user-agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.0.0 Safari/537.36");
                    connection.setRequestProperty("referer", url);

                    if (connection.getResponseCode() != 416) {
                        byte[] bytes = new byte[8 * 1024];
                        InputStream stream = new BufferedInputStream(connection.getInputStream());
                        FileOutputStream outputStream = new FileOutputStream(downloadFile, true);
                        int count;

                        bytesReceived.addAndGet(downloadFile.length());
                        while (!DownloadTask.this.isCanceled && (count = stream.read(bytes)) != -1) {
                            bytesReceived.addAndGet(count);
                            outputStream.write(bytes, 0, count);
                            notifyProgress(bytesReceived.get(), fileLength);

                        }

                        if (DownloadTask.this.isCanceled)
                            LogHelper.d(TAG, partFileName + " is interrupted by the user");
                        else
                            LogHelper.d(TAG, partFileName + " successfully downloaded!!!");

                        outputStream.flush();
                        outputStream.close();
                        stream.close();
                    }
                    return true;
                } catch (Exception e) {
                    return false;
                }

            }).subscribeOn(Schedulers.from(executorService));

            futures.add(uCompletableFuture);

        }

        LogHelper.d(TAG, "downloadWithParallel: Waiting for all files to be downloaded");
        boolean isAllSuccessfullyCompleted = Single.zip(futures, objects -> {
                    for (Object obj : objects) {
                        if (!(boolean) obj) {
                            return false;
                        }
                    }
                    return true;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .blockingGet();


        if (isAllSuccessfullyCompleted) {
            LogHelper.d(TAG, "downloadWithParallel: Merging all files ");
            FileOutputStream fout = new FileOutputStream(file);
            for (int i = 0; i < NUM_THREADS; i++) {
                byte[] bytes = new byte[BUFFER_SIZE];
                File partFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), String.format(partNameFormat, fileName, bitrate, NUM_THREADS, i));
                FileInputStream fin = new FileInputStream(partFile);
                int count;
                while (!DownloadTask.this.isCanceled && (count = fin.read(bytes)) != -1) {
                    fout.write(bytes, 0, count);
                }
                fin.close();
                partFile.delete();
            }
            fout.close();
        } else if (!isCanceled) {
            throw new InterruptedException("Failed to download all files");
        }

    }

    private int getIndex(int bitrate, List<StreamingData.AdaptiveAudioFormat> audioStreams) {
        return audioStreams.size() - 2;
    }

    long prevTime = -1, currentTime;

    void notifyProgress(long received, long total) {
        if (isTerminate) return;
        currentTime = System.currentTimeMillis();
        if (prevTime == -1) prevTime = System.currentTimeMillis();
        else if (currentTime - prevTime < 250) return;
        prevTime = currentTime;
        int progress = (int) (((double) received / total) * 100);
        notificationBuilder.setContentInfo(progress + "%");
        notificationBuilder.setProgress(100, progress, false);
        notificationBuilder.setOngoing(true);
        manager.notify(taskId, notificationBuilder.build());
        LogHelper.d(TAG, String.format(Locale.US, "progress : %d %%", progress));
        updateProgress(received, total);
        updateStatus(DownloadStatus.DOWNLOADING);
        DownloadRepository.getInstance(context).getDownloadDao().updateLength(downloadId, received);
    }

    void updateProgress(long current, long total) {
        downloadProgressLiveData.postValue(new DownloadProgress(current, total));
    }

    void updateStatus(DownloadStatus status) {
        DownloadRepository.getInstance(context).getDownloadDao().updateStatus(downloadId, status.name());
        downloadStatusLiveData.postValue(status);
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
    public void notifyProgressPaused(String status) {
        LogHelper.d(TAG, "notifyProgressPaused: ");
        if (notificationBuilder == null)
            return;
        isTerminate = true;
        prevTime = -1;
        PendingIntent dFilePendingIntent = getPendingIntentByAction(Action.RESUME);
        notificationBuilder.mActions.set(0, new NotificationCompat.Action(R.drawable.icon_play, "Resume", dFilePendingIntent));
        notificationBuilder.setContentText(status);
        notificationBuilder.setOngoing(false);
        notificationBuilder.setContentInfo("");
        notificationBuilder.setSmallIcon(R.drawable.ic_pause_24);
        notificationBuilder.setProgress(0, 0, false);
        manager.notify(taskId, notificationBuilder.build());
    }

    public int addToDownloads(VideoDetails videoDetails, int bitrate) {
        Download dFile = new Download();
        dFile.setVideoId(videoId);
        dFile.setFileImageUrl(videoDetails.getVideoData().getThumbnail().getThumbnails().get(1).getUrl());
        dFile.setFileName(videoDetails.getVideoData().getTitle());
        dFile.setFileSubText(videoDetails.getVideoData().getAuthor());
        dFile.setBitrate(bitrate);
        dFile.setStatus(DownloadStatus.DOWNLOADING);
        dFile.setExtension(MimeTypeMap.getSingleton().getExtensionFromMimeType(MediaFormat.MIMETYPE_AUDIO_MPEG));
        DownloadDao downloadDao = DownloadRepository.getInstance(context).getDownloadDao();
        return (int) downloadDao.insert(dFile);
    }

    public void cancel() {
        this.isCanceled = true;
        if(fFmpegSession != null) {
            FFMpegUtil.cancel(fFmpegSession);
        }
        DownloadTask.this.interrupt();
        notifyProgressPaused("Paused");
        LogHelper.d(TAG, "cancel: Task id %s is interrupted", videoId);
    }

    enum Action {
        RESUME, PAUSE
    }

    public static class DownloadProgress {
        public long receivedBytes;
        public long totalBytes;

        public DownloadProgress(long receivedBytes, long totalBytes) {
            this.receivedBytes = receivedBytes;
            this.totalBytes = totalBytes;
        }

        public DownloadProgress() {
        }
    }
}