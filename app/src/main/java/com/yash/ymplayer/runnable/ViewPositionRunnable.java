package com.yash.ymplayer.runnable;

import android.view.View;

public abstract class ViewPositionRunnable implements Runnable {
    private View view;
    private double x;
    private double y;

    public ViewPositionRunnable(View view, double x, double y) {
        this.view = view;
        this.x = x;
        this.y = y;
    }
}
