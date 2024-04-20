package com.yash.ymplayer.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ThreadPool {

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private ThreadPool() {}

    private static ThreadPool instance;
    public static ThreadPool getInstance() {
        if(instance == null)
            instance = new ThreadPool();
        return instance;
    }

    public ExecutorService getExecutor() {
        return executorService;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutorService;
    }


}
