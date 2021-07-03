package com.yash.ymplayer.data;

import android.net.Uri;

import com.yash.logging.LogHelper;

import java.util.HashMap;
import java.util.Map;

public class UriCache {
    private static final String TAG = "UriCache";
    Map<String, String> uriLowMap;
    Map<String, String> uriMediumMap;
    Map<String, String> uriHighMap;
    Map<String, Long> uriLengthMap;

    String[] ids;
    int size;
    int queuePos;

    public UriCache(int size) {
        this.uriLowMap = new HashMap<>();
        this.uriMediumMap = new HashMap<>();
        this.uriHighMap = new HashMap<>();
        this.uriLengthMap = new HashMap<>();
        ids = new String[size];
        this.size = size;
        queuePos = -1;
    }

//    public void pushUri(String id, Uri[] uris) {
//        queuePos = getQueuePosition();
//        String prevId = ids[queuePos];
//        if (prevId != null) {
//            uriLowMap.remove(prevId);
//            uriMediumMap.remove(prevId);
//            uriHighMap.remove(prevId);
//            LogHelper.d(TAG, "pushUri: removed uri id:"+prevId);
//        }
//        uriLowMap.put(id, uris[0]);
//        uriMediumMap.put(id, uris[1]);
//        uriHighMap.put(id, uris[2]);
//        ids[queuePos] = id;
//
//    }

    public void pushUri(String id, String lowUrl, String mediumUrl, String highUrl, Long length) {
        queuePos = getQueuePosition();
        String prevId = ids[queuePos];
        if (prevId != null) {
            uriLowMap.remove(prevId);
            uriMediumMap.remove(prevId);
            uriHighMap.remove(prevId);
            uriLengthMap.remove(prevId);
            LogHelper.d(TAG, "pushUri: removed uri id:" + prevId);
        }
        uriLowMap.put(id, lowUrl);
        uriMediumMap.put(id, mediumUrl);
        uriHighMap.put(id, highUrl);
        uriLengthMap.put(id, length);
        ids[queuePos] = id;

    }

    public Uri getUri(String id, int quality) {
        switch (quality) {
            case 1:
                if (!uriLowMap.containsKey(id))
                    return null;
                return Uri.parse(uriLowMap.get(id));
            case 2:
                if (!uriMediumMap.containsKey(id))
                    return null;
                return Uri.parse(uriMediumMap.get(id));
            case 3:
                if (!uriHighMap.containsKey(id))
                    return null;
                return Uri.parse(uriHighMap.get(id));
            default:
                return null;
        }

    }

    public long getLength(String id) {
        if (uriLengthMap.containsKey(id))
            return uriLengthMap.get(id);
        else return 0L;
    }

    public boolean isInCache(String id, int quality) {

        switch (quality) {
            case 1:
                return uriLowMap.containsKey(id);
            case 2:
                return uriMediumMap.containsKey(id);
            case 3:
                return uriHighMap.containsKey(id);
            default:
                return false;
        }
    }

    int getQueuePosition() {
        return (queuePos + 1) % size;
    }

}
