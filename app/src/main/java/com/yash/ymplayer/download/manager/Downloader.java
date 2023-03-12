package com.yash.ymplayer.download.manager;

import android.database.Observable;

import androidx.lifecycle.Observer;

import com.yash.logging.LogHelper;
import com.yash.ymplayer.download.manager.constants.DownloadStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Downloader {
    private static final String TAG = "Downloader";
    Queue<DownloadTask> taskQueue;
    Map<String, DownloadTask> runningTasks;
    int maxRunningTasks;
    Queue<Integer> taskIds;
    DownloadService downloadService;
    Map<String, Integer> taskIdMap = new HashMap<>();
//    Map<String, List<Observer<DownloadTask.DownloadProgress>>> progressObservableMap = new HashMap<>();
//    Map<String, List<Observer<DownloadStatus>>> statusObservableMap = new HashMap<>();

    private static Downloader instance;

    public static Downloader getInstance() {
        return instance;
    }

    public static Downloader getInstance(DownloadService downloadService, int maxRunningTasks) {
        if (instance == null)
            instance = new Downloader(maxRunningTasks);
        instance.downloadService = downloadService;
        return instance;
    }

    private Downloader(int maxRunningTasks) {
        this.maxRunningTasks = maxRunningTasks;
        taskQueue = new ArrayDeque<>();
        runningTasks = new HashMap<>();
        taskIds = new ArrayDeque<>(100);
        for (int i = 100; i < 200; i++)
            taskIds.add(i);
    }


    void enqueue(DownloadTask task) {
        LogHelper.d(TAG, "enqueue:  %s", task.getVideoId());
        String uniqueId = getUniqueId(task);
        LogHelper.d(TAG, "enqueue: Unique id - %s", uniqueId);
        Integer taskId = taskIdMap.get(uniqueId);
        if (taskId != null) {
            LogHelper.d(TAG, "enqueue: Existing task id found : %s for id - %s", task, uniqueId);
        }
        Integer id = taskId == null ? taskIds.poll() : taskId;
        if (id == null) {
            LogHelper.d(TAG, "enqueue: Worker instances not available");
            return;
        }
        task.setCallbackObject(this);
        task.setTaskId(id);
        taskIdMap.put(uniqueId, id);
        taskQueue.add(task);
        execute();
    }

    void resume(DownloadTask thread) {
        thread.setCallbackObject(this);
        taskQueue.add(thread);
        execute();
    }

    void execute() {
        LogHelper.d(TAG, "execute:");
        if (runningTasks.size() == maxRunningTasks) {
            LogHelper.d(TAG, "execute: Max running task reached");
            return;
        }
        if (taskQueue.isEmpty()) {
            LogHelper.d(TAG, "execute: Task queue is empty to execute");
            return;
        }
        DownloadTask thread = taskQueue.poll();
        if (thread == null) {
            LogHelper.d(TAG, "execute: Task unavailable");
            return;
        }
        String uniqueId = getUniqueId(thread);
        Integer taskId = taskIdMap.get(uniqueId);
        thread.setTaskId(taskId != null ? taskId : 100);
        runningTasks.put(uniqueId, thread);
        LogHelper.d(TAG, "execute:  running task with id %s", uniqueId);
        thread.start();
    }

    void taskFinished(DownloadTask task) {
        String uniqueId = getUniqueId(task);
        LogHelper.d(TAG, "taskFinished id - %s", uniqueId);
        taskIds.add(task.getTaskId());
        taskIdMap.remove(uniqueId);
        runningTasks.remove(uniqueId);
        if (!taskQueue.isEmpty())
            execute();
        else if (runningTasks.isEmpty())
            downloadService.stopSelf();
    }

    public void pause(int downloadId, String videoId) {
        Set<String> keys = new HashSet<>(runningTasks.keySet());
        for (String key : keys) {
            DownloadTask task = runningTasks.get(key);
            if (task == null)
                continue;
            if (task.getVideoId().equals(videoId) && task.getDownloadId() == downloadId) {
                runningTasks.remove(key);
                task.cancel();

                LogHelper.d(TAG, "Task paused - %s", key);
                execute();

            }
        }
    }

    public DownloadTask findTask(String videoId, int bitrate) {
        String uniqueId = getUniqueId(videoId, bitrate);
        return runningTasks.get(uniqueId);
    }

//    public void addStatusObserver(String videoId, Observer<DownloadStatus> observer) {
//        List<Observer<DownloadStatus>> observers = statusObservableMap.get(videoId);
//        if(observers == null)
//            observers = new ArrayList<>();
//        observers.add(observer);
//        statusObservableMap.put(videoId, observers);
//    }
//    public void addProgressObserver(String videoId, Observer<DownloadTask.DownloadProgress> observer) {
//        List<Observer<DownloadTask.DownloadProgress>> observers = progressObservableMap.get(videoId);
//        if(observers == null)
//            observers = new ArrayList<>();
//        observers.add(observer);
//        progressObservableMap.put(videoId, observers);
//    }

    public String getUniqueId(DownloadTask task) {
        return getUniqueId(task.getVideoId(), task.getBitrate());
    }

    public String getUniqueId(String videoId, int bitrate) {
        return videoId + "_" + bitrate;
    }
}

