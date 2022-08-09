package com.yash.ymplayer.util;

public class CommonUtil {
    public static String extractId(String mediaId) {
        if (mediaId == null)
            return null;
        String[] splits = mediaId.split("[/|]");
        return splits[splits.length - 1];
    }
}
