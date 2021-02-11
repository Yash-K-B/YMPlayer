package com.yash.ymplayer;

import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

public interface ActivityActionProvider {
    /**
     * Method to set toolbar from fragments in Current Screen
     *
     * @param toolbar Toolbar to attach, null for default
     * @param title   Title of Activity, null for default
     */
    void setCustomToolbar(@Nullable Toolbar toolbar, @Nullable String title);

    /**
     * The method provide the interface to set name in the Navigation Drawer
     *
     * @param name name of the user
     */
    void setUserName(String name);

    /**
     * This method sends the custom action to the MediaSession through
     * MediaController.
     *
     * @param action the action to be done
     * @param extras the arguments if any, null for no arguments
     */
    void sendActionToMediaSession(String action, Bundle extras);

    void interactWithMediaSession(Callback callback);

    interface Callback {
        void trigger(MediaControllerCompat mediaController);
    }
}
