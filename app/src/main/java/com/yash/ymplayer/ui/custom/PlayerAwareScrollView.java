package com.yash.ymplayer.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ScrollView;

public class PlayerAwareScrollView extends ScrollView implements PlayerAware {
    public PlayerAwareScrollView(Context context) {
        super(context);
    }

    public PlayerAwareScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerAwareScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PlayerAwareScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public View getView() {
        return this;
    }
}
