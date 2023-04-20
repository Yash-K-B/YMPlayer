package com.yash.ymplayer.data;

import com.yash.ymplayer.constant.Status;

public class DataStatusWrapper<T> {
    private Status status;
    private T data;
    private String error;
}
