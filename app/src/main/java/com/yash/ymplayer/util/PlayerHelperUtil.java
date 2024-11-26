package com.yash.ymplayer.util;

import com.yash.ymplayer.constant.Constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerHelperUtil {
    public static boolean needWatchNextItems(String uri) {
        return StringUtil.hasText(uri) && (isSharedYoutube(uri) || isSearchedYoutube(uri));
    }

    public static boolean isSharedYoutube(String uri) {
        return uri.startsWith(Constants.PREFIX_SHARED);
    }

    public static boolean isSearchedYoutube(String uri) {
        return uri.startsWith(Constants.PREFIX_SEARCH);
    }

}
