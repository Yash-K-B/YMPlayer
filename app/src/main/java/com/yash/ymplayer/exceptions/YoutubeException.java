package com.yash.ymplayer.exceptions;

public class YoutubeException extends Exception {
    public YoutubeException(String message) {
        super(message);
    }

    public YoutubeException(String message, Throwable cause) {
        super(message, cause);
    }

    public YoutubeException(Throwable cause) {
        super(cause);
    }
}
