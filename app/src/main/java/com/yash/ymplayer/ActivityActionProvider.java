package com.yash.ymplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

public interface ActivityActionProvider {
    /**
     * Method to set toolbar from fragments in Current Screen
     * @param toolbar Toolbar to attach, null for default
     * @param title   Title of Activity, null for default
     */
    void setCustomToolbar(@Nullable Toolbar toolbar, @Nullable String title);

    /**
     * The method provide the interface to set name in the Navigation Drawer
     * @param name name of the user
     */
    void setUserName(String name);
}
