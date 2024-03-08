package com.yash.ymplayer.pool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
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


}
