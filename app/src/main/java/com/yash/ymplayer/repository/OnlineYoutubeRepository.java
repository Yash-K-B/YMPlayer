package com.yash.ymplayer.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.models.PopularPlaylist;
import com.yash.ymplayer.models.PopularPlaylists;
import com.yash.ymplayer.models.YoutubePlaylist;
import com.yash.ymplayer.storage.AudioProvider;
import com.yash.ymplayer.ui.youtube.livepage.PagedResponse;
import com.yash.ymplayer.util.YoutubeSong;
import com.yash.youtube_extractor.Extractor;
import com.yash.youtube_extractor.ExtractorHelper;
import com.yash.youtube_extractor.constants.ContinuationType;
import com.yash.youtube_extractor.exceptions.ExtractionException;
import com.yash.youtube_extractor.models.VideoData;
import com.yash.youtube_extractor.models.VideoDetails;
import com.yash.youtube_extractor.models.YoutubeResponse;
import com.yash.youtube_extractor.utility.CollectionUtility;
import com.yash.youtube_extractor.utility.CommonUtility;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.android.volley.Request.Method.GET;

public class OnlineYoutubeRepository {
    private static final String TAG = "OnlineYoutubeRepository";
    Map<String, TopTracksYoutubeSongsCache> topTracksCache;
    Map<String, List<YoutubeSong>> loadedTracksMap;
    Map<String, List<Pair<String, List<com.yash.youtube_extractor.models.YoutubePlaylist>>>> loadedPlaylistsMap;
    Context context;
    static OnlineYoutubeRepository instance;
    RequestQueue requestQueue;
    RequestedTracksLoadedCallback callback;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    OnlineYoutubeRepository(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(context);
        this.topTracksCache = new LinkedHashMap<>();
        this.loadedTracksMap = new HashMap<>();
        this.loadedPlaylistsMap = new HashMap<>();
    }

    //singleton
    public static OnlineYoutubeRepository getInstance(Context context) {
        if (instance == null)
            instance = new OnlineYoutubeRepository(context);
        return instance;
    }

    public void topTracks(String pageToken, RequestedTracksLoadedCallback callback) {
        this.callback = callback;
        LogHelper.d(TAG, "topTracks: Extraction");
        String url = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&fields=prevPageToken,nextPageToken,items(snippet(title,thumbnails(default,medium,high),channelTitle,resourceId))&playlistId=PL4fGSI1pDJn40WjZ6utkIuj2rNg-7iGsq&maxResults=50&key=AIzaSyAdfvLnL55J-5dOwiL_IDF5PydkF3r2jQA";
        if (!pageToken.equals("-1"))
            url += "&pageToken=" + pageToken;
        if (topTracksCache != null && topTracksCache.containsKey(pageToken)) {
            LogHelper.d(TAG, "topTracks: Saved Network call due to caching token: " + pageToken);
            TopTracksYoutubeSongsCache cache = topTracksCache.get(pageToken);
            callback.onLoaded(cache.getTracks(), cache.getPrevPageToken(), cache.getNextPageToken());
        } else {
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(GET, url, null, response -> {
                String responseJson = response.toString();
                LogHelper.d(TAG, "topTracks: response found : " + responseJson);
                Gson gson = new Gson();
                YoutubePlaylist playlist = gson.fromJson(responseJson, YoutubePlaylist.class);
                List<YoutubeSong> tracks = new ArrayList<>();
                LogHelper.d(TAG, "onResponse: prevPageToken : " + playlist.getPrevPageToken());
                for (int i = 0; i < playlist.getItems().size(); i++) {
                    YoutubePlaylist.PlayListItem.Snippet snippet = playlist.getItems().get(i).getSnippet();
                    if (snippet == null || "Deleted video".equals(snippet.getTitle()))
                        continue;
                    //LogHelper.d(TAG, "onResponse: " + playlist.getItems().get(i).getSnippet().getTitle());
                    String title = snippet.getTitle();
                    String videoId = snippet.getResourceId().getVideoId();
                    String channelName = snippet.getChannelTitle();
                    YoutubePlaylist.PlayListItem.Snippet.Thumbnails thumbnails = snippet.getThumbnails();
                    String art_small = null;
                    String art_medium = null;
                    String art_high = null;
                    if (thumbnails != null) {
                        art_small = thumbnails.getDefault() != null ? thumbnails.getDefault().getUrl() : null;
                        art_medium = thumbnails.getMedium() != null ? thumbnails.getMedium().getUrl() : null;
                        art_high = thumbnails.getHigh() != null ? thumbnails.getHigh().getUrl() : null;
                    }
                    YoutubeSong song = new YoutubeSong(title, videoId, channelName, art_small, art_medium, art_high);
                    song.setChannelDesc("Top Tracks");
                    tracks.add(song);
                }
                loadSongDuration(tracks, callback, pageToken, playlist.getPrevPageToken(), playlist.getNextPageToken());
            }, error -> LogHelper.d(TAG, "onErrorResponse: " + error)) {
                @Override
                protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                    try {
                        Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                        if (cacheEntry == null) {
                            cacheEntry = new Cache.Entry();
                        }
                        final long cacheHitButRefreshed = 12 * 60 * 60 * 1000; // in 12 hour cache will be hit, but also refreshed on background
                        final long cacheExpired = 24 * 60 * 60 * 1000; // in 1 days this cache entry expires completely
                        long now = System.currentTimeMillis();
                        final long softExpire = now + cacheHitButRefreshed;
                        final long ttl = now + cacheExpired;
                        cacheEntry.data = response.data;
                        cacheEntry.softTtl = softExpire;
                        cacheEntry.ttl = ttl;
                        String headerValue;
                        headerValue = response.headers.get("Date");
                        if (headerValue != null) {
                            cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                        }
                        headerValue = response.headers.get("Last-Modified");
                        if (headerValue != null) {
                            cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                        }
                        cacheEntry.responseHeaders = response.headers;
                        final String jsonString = new String(response.data,
                                HttpHeaderParser.parseCharset(response.headers));
                        return Response.success(new JSONObject(jsonString), cacheEntry);
                    } catch (UnsupportedEncodingException | JSONException e) {
                        return Response.error(new ParseError(e));
                    }
                }
            };
            requestQueue.add(jsonObjectRequest);
        }

    }

    private void loadSongDuration(List<YoutubeSong> songs, RequestedTracksLoadedCallback callback, String pageToken, String prevToken, String nextToken) {
        StringBuilder ids = new StringBuilder();
        for (YoutubeSong song : songs)
            ids.append(song.getVideoId()).append(",");
        if (ids.length() != 0)
            ids.deleteCharAt(ids.length() - 1);
        String url = "https://www.googleapis.com/youtube/v3/videos?part=contentDetails,snippet&maxResults=50&fields=items(snippet(channelTitle),contentDetails(duration))&key=AIzaSyAdfvLnL55J-5dOwiL_IDF5PydkF3r2jQA" + "&id=" + ids;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(GET, url, null, response -> {
            //LogHelper.d(TAG, "onResponse: " + response);
            try {
                JSONArray jsonArray = response.getJSONArray("items");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject channelNameObj = jsonArray.getJSONObject(i).getJSONObject("snippet");
                    JSONObject durationObj = jsonArray.getJSONObject(i).getJSONObject("contentDetails");
                    String channelName = channelNameObj.getString("channelTitle");
                    String duration = durationObj.getString("duration").replace("PT", "").replace("H", ":").replace("M", ":").replace("S", "");
                    String[] splits = duration.split(":", 3);
                    long song_duration = 0L;
                    int pos = splits.length - 1;
                    for (int r = pos; r >= 0; r--) {
                        if (splits[r].isEmpty()) splits[r] = "0";
                        song_duration += Long.parseLong(splits[r]) * Math.pow(60, pos - r);
                    }
                    songs.get(i).setDurationMillis(song_duration * 1000);
                    songs.get(i).setChannelTitle(channelName);
                    //LogHelper.d(TAG, "onResponse: duration in secs: " + song_duration + " s");
                }
                topTracksCache.put(pageToken, new TopTracksYoutubeSongsCache(prevToken, nextToken, songs));
                LogHelper.d(TAG, "loadSongDuration: " + (this.callback == callback));
                if (this.callback == callback)
                    callback.onLoaded(songs, prevToken, nextToken);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }, error -> LogHelper.d(TAG, "onErrorResponse: ")) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }
                    final long cacheHitButRefreshed = 12 * 60 * 60 * 1000; // in 12 hour cache will be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 1 days this cache entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;
                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;
                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers));
                    return Response.success(new JSONObject(jsonString), cacheEntry);
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    public List<MediaSessionCompat.QueueItem> getPlayingQueue() {
        List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
        long queueId = 0L;
        if (topTracksCache != null) {
            for (String key : topTracksCache.keySet())
                for (YoutubeSong song : topTracksCache.get(key).getTracks()) {
                    Bundle extras = new Bundle();
                    extras.putLong("duration", song.getDurationMillis());
                    MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                            .setMediaId(song.getVideoId())
                            .setTitle(song.getTitle())
                            .setSubtitle(song.getChannelTitle())
                            .setDescription(song.getChannelDesc())
                            .setIconUri(Uri.parse(song.getArt_url_high()))
                            .setExtras(extras)
                            .build()
                            , queueId++);
                    mediaItems.add(queueItem);
                }
        }
        return mediaItems;
    }

    public interface RequestedTracksLoadedCallback {
        void onLoaded(List<YoutubeSong> tracks, String prevToken, String nextToken);
    }

    static class TopTracksYoutubeSongsCache {
        String prevPageToken, nextPageToken;
        List<YoutubeSong> tracks;

        public TopTracksYoutubeSongsCache(String prevPageToken, String nextPageToken, List<YoutubeSong> tracks) {
            this.prevPageToken = prevPageToken;
            this.nextPageToken = nextPageToken;
            this.tracks = tracks;
        }

        public String getPrevPageToken() {
            return prevPageToken;
        }

        public String getNextPageToken() {
            return nextPageToken;
        }

        public List<YoutubeSong> getTracks() {
            return tracks;
        }
    }


    /*                 --------------------------------------------------------                             */
    public void getPopularPlaylist(PlaylistsLoadedCallback callback) {
        String url = "https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&fields=items(id,snippet(localized,thumbnails(medium,standard)),contentDetails)&maxResults=50&id=RDCLAK5uy_n9Fbdw7e6ap-98_A-8JYBmPv64v-Uaq1g,RDCLAK5uy_kuo_NioExeUmw07dFf8BzQ64DFFTlgE7Q,RDCLAK5uy_mOvRWCE7v4C98UgkSVh5FTlD3osGjolas,RDCLAK5uy_lyVnWI5JnuwKJiuE-n1x-Un0mj9WlEyZw,RDCLAK5uy_nTbyVypdXPQd00z15bTWjZr7pG-26yyQ4,RDCLAK5uy_lj-zBExVYl7YN_NxXboDIh4A-wKGfgzNY,RDCLAK5uy_n17q7_2dwfDqWckpccDyTTkZ-g03jXuII,RDCLAK5uy_nhLiD_PquxQnzA35YpoaaAUv2ikZuYFgw,RDCLAK5uy_nT-zkEpc2x7AVVP0XV9JvHSfkFsOtGMR8,RDCLAK5uy_mPBQePobkU9UZ100tOTfvTCdwWOHoiiPo,RDCLAK5uy_kjNBBWqyQ_Cy14B0P4xrcKgd39CRjXXKk&key=AIzaSyAdfvLnL55J-5dOwiL_IDF5PydkF3r2jQA";
        JsonObjectRequest request = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                LogHelper.d(TAG, "onResponse: getPopularPlaylist");
                Gson gson = new Gson();
                PopularPlaylists playlists = gson.fromJson(response.toString(), PopularPlaylists.class);
                callback.onLoaded(playlists.getItems());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LogHelper.d(TAG, "onErrorResponse: ");
                callback.onError();
            }
        }) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }
                    final long cacheHitButRefreshed = 12 * 60 * 60 * 1000; // in 12 hour cache will be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 1 days this cache entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;
                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;
                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers));
                    return Response.success(new JSONObject(jsonString), cacheEntry);
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };
        requestQueue.add(request);
    }

    public interface PlaylistsLoadedCallback {
        void onLoaded(List<PopularPlaylist> playlists);

        void onError();
    }

    /*                     -----------------------------------------------------------------------                                */
    public void getAllTimeHitPlaylist(PlaylistsLoadedCallback callback) {
        String url = "https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&fields=items(id,snippet(localized,thumbnails(medium,standard)),contentDetails)&maxResults=50&id=RDCLAK5uy_m3_ta3qbw52SzpnQMqfph0yPeyrBaJN-g,RDCLAK5uy_kGe1PylaE2pmTrZZZh8PWLlh7eelpeZwo,RDCLAK5uy_lmVWEkkypRcXdChTg534p8SRNSfkhj3rA,RDCLAK5uy_nZiG9ehz_MQoWQxY5yElsLHCcG0tv9PRg,RDCLAK5uy_nvOP2zYXgysTuhzHxjDja5-tEyp_k93SQ,RDCLAK5uy_loJPSV6vieb07DOApAZTqxQ2xQ85p-ix8,RDCLAK5uy_mVRuj5egfh21e-pXyA3ymx_0p4Xlg-c0I,RDCLAK5uy_nmS3YoxSwVVQk9lEQJ0UX4ZCjXsW_psU8,RDCLAK5uy_nRFg09Ti0dBPioDAiLA3HS3Z3lDFW6bhQ,RDCLAK5uy_nwEDp_RJ7SwuHtC3fxBGOBXW7v2YPrrks,RDCLAK5uy_kDYZ6-tSN8D-D0w0RFdYqMort1x3X22D4,RDCLAK5uy_nBA5jbzVGlHk_a9Bu32skKxnU3T--0y4M&key=AIzaSyAdfvLnL55J-5dOwiL_IDF5PydkF3r2jQA";
        JsonObjectRequest request = new JsonObjectRequest(url, null, response -> {
            Gson gson = new Gson();
            PopularPlaylists playlists = gson.fromJson(response.toString(), PopularPlaylists.class);
            callback.onLoaded(playlists.getItems());
        }, error -> {
            callback.onError();
        }) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }
                    final long cacheHitButRefreshed = 12 * 60 * 60 * 1000; // in 12 hour cache will be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 1 days this cache entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;
                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;
                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers));
                    return Response.success(new JSONObject(jsonString), cacheEntry);
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };
        requestQueue.add(request);
    }


    public void getPlaylistTracks(String id, String desc, String pageToken, TracksLoadedCallback callback) {
        LogHelper.d(TAG, "topTracks: Extraction");
        String url = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&fields=prevPageToken,nextPageToken,items(snippet(title,thumbnails(default,medium,high),channelTitle,resourceId))&playlistId=" + id + "&maxResults=50&key=AIzaSyAdfvLnL55J-5dOwiL_IDF5PydkF3r2jQA";
        if (!pageToken.equals("-1"))
            url += "&pageToken=" + pageToken;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(GET, url, null, response -> {
            Gson gson = new Gson();
            YoutubePlaylist playlist = gson.fromJson(response.toString(), YoutubePlaylist.class);
            List<YoutubeSong> tracks = new ArrayList<>();
            LogHelper.d(TAG, "onResponse: prevPageToken : " + playlist.getPrevPageToken());
            for (int i = 0; i < playlist.getItems().size(); i++) {
                //LogHelper.d(TAG, "onResponse: " + playlist.getItems().get(i).getSnippet().getTitle());
                YoutubePlaylist.PlayListItem.Snippet snippet = playlist.getItems().get(i).getSnippet();
                if (snippet == null || "Deleted video".equals(snippet.getTitle()))
                    continue;
                //LogHelper.d(TAG, "onResponse: " + playlist.getItems().get(i).getSnippet().getTitle());
                String title = snippet.getTitle();
                String videoId = snippet.getResourceId().getVideoId();
                String channelName = snippet.getChannelTitle();
                YoutubePlaylist.PlayListItem.Snippet.Thumbnails thumbnails = snippet.getThumbnails();
                String art_small = null;
                String art_medium = null;
                String art_high = null;
                if (thumbnails != null) {
                    art_small = thumbnails.getDefault() != null ? thumbnails.getDefault().getUrl() : null;
                    art_medium = thumbnails.getMedium() != null ? thumbnails.getMedium().getUrl() : null;
                    art_high = thumbnails.getHigh() != null ? thumbnails.getHigh().getUrl() : null;
                }
                YoutubeSong song = new YoutubeSong(title, videoId, channelName, art_small, art_medium, art_high);
                song.setChannelDesc(desc);
                tracks.add(song);
            }
            loadTracksDuration(tracks, id, callback);
        }, error -> {
            LogHelper.d(TAG, "onErrorResponse: " + error);
            callback.onError(error);
        }) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }
                    final long cacheHitButRefreshed = 12 * 60 * 60 * 1000; // in 12 hour cache will be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 1 days this cache entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;
                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;
                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers));
                    return Response.success(new JSONObject(jsonString), cacheEntry);
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };
        requestQueue.add(jsonObjectRequest);
    }


    private void loadTracksDuration(List<YoutubeSong> songs, String id, TracksLoadedCallback callback) {
        StringBuilder ids = new StringBuilder();
        for (YoutubeSong song : songs)
            ids.append(song.getVideoId()).append(",");
        if (ids.length() != 0)
            ids.deleteCharAt(ids.length() - 1);
        String url = "https://www.googleapis.com/youtube/v3/videos?part=contentDetails,snippet&maxResults=50&fields=items(snippet(channelTitle),contentDetails(duration))&key=AIzaSyAdfvLnL55J-5dOwiL_IDF5PydkF3r2jQA" + "&id=" + ids;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(GET, url, null, response -> {
            //LogHelper.d(TAG, "onResponse: " + response);
            try {
                JSONArray jsonArray = response.getJSONArray("items");
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject channelNameObj = jsonArray.getJSONObject(i).getJSONObject("snippet");
                    JSONObject durationObj = jsonArray.getJSONObject(i).getJSONObject("contentDetails");
                    String channelName = channelNameObj.getString("channelTitle");
                    String duration = durationObj.getString("duration").replace("PT", "").replace("H", ":").replace("M", ":").replace("S", "");
                    String[] splits = duration.split(":", 3);
                    long song_duration = 0L;
                    int pos = splits.length - 1;
                    for (int r = pos; r >= 0; r--) {
                        if (splits[r].isEmpty()) splits[r] = "0";
                        song_duration += Long.parseLong(splits[r]) * Math.pow(60, pos - r);
                    }
                    songs.get(i).setDurationMillis(song_duration * 1000);
                    songs.get(i).setChannelTitle(channelName);
                    //LogHelper.d(TAG, "onResponse: duration in secs: " + song_duration + " s");
                }
                loadedTracksMap.put(id, songs);
                callback.onLoaded(songs);

            } catch (JSONException e) {
                e.printStackTrace();
                callback.onError(e);
            }

        }, error -> LogHelper.d(TAG, "onErrorResponse: ")) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }
                    final long cacheHitButRefreshed = 12 * 60 * 60 * 1000; // in 12 hour cache will be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 1 days this cache entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;
                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;
                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers));
                    return Response.success(new JSONObject(jsonString), cacheEntry);
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };
        requestQueue.add(jsonObjectRequest);
    }


    public interface QueueLoadedCallback {
        void onLoaded(List<MediaSessionCompat.QueueItem> queueItems);

        <E extends Exception> void onError(E e);
    }

    public interface TracksLoadedCallback {
        void onLoaded(List<YoutubeSong> songs);

        <E extends Exception> void onError(E e);
    }

    public interface PlaylistLoadedCallback {
        void onLoaded(List<Pair<String, List<com.yash.youtube_extractor.models.YoutubePlaylist>>> playlistsByCategory);

        <E extends Exception> void onError(E e);
    }


    public List<MediaSessionCompat.QueueItem> getPlayingQueue(String uri) {
        String id = uri.split("[|]")[0];
        if ("TOP_TRACKS".equals(id)) {
            return getPlayingQueue();
        } else {
            List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
            long queueId = 0L;
            if (loadedTracksMap != null) {
                if (loadedTracksMap.containsKey(id))
                    for (YoutubeSong song : Objects.requireNonNull(loadedTracksMap.get(id))) {
                        Bundle extras = new Bundle();
                        extras.putLong("duration", song.getDurationMillis());
                        MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                                .setMediaId(song.getVideoId())
                                .setTitle(song.getTitle())
                                .setSubtitle(song.getChannelTitle())
                                .setDescription(song.getChannelDesc())
                                .setIconUri(Uri.parse(song.getArt_url_high()))
                                .setExtras(extras)
                                .build()
                                , queueId++);
                        mediaItems.add(queueItem);
                    }
                else LogHelper.d(TAG, "getPlayingQueue: No result found");
            }
            return mediaItems;
        }

    }

    public List<MediaSessionCompat.QueueItem> getPlayingQueueSingle(String uri) {
        String id = uri.split("[|]")[0];
        String videoId = uri.split("[|]")[1];
        if ("TOP_TRACKS".equals(id)) {
            List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
            long queueId = 0L;
            if (topTracksCache != null) {
                for (String key : topTracksCache.keySet())
                    for (YoutubeSong song : topTracksCache.get(key).getTracks()) {
                        if (song.getVideoId().equals(videoId)) {
                            Bundle extras = new Bundle();
                            extras.putLong("duration", song.getDurationMillis());
                            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                                    .setMediaId(song.getVideoId())
                                    .setTitle(song.getTitle())
                                    .setSubtitle(song.getChannelTitle())
                                    .setDescription(song.getChannelDesc())
                                    .setIconUri(Uri.parse(song.getArt_url_high()))
                                    .setExtras(extras)
                                    .build()
                                    , queueId++);
                            mediaItems.add(queueItem);
                        }
                    }
            }
            return mediaItems;
        } else {
            List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
            long queueId = 0L;
            if (loadedTracksMap != null) {
                if (loadedTracksMap.containsKey(id)) {
                    for (YoutubeSong song : Objects.requireNonNull(loadedTracksMap.get(id))) {
                        if (song.getVideoId().equals(videoId)) {
                            Bundle extras = new Bundle();
                            extras.putLong("duration", song.getDurationMillis());
                            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                                    .setMediaId(song.getVideoId())
                                    .setTitle(song.getTitle())
                                    .setSubtitle(song.getChannelTitle())
                                    .setDescription(song.getChannelDesc())
                                    .setIconUri(Uri.parse(song.getArt_url_high()))
                                    .setExtras(extras)
                                    .build()
                                    , queueId++);
                            mediaItems.add(queueItem);
                        }
                    }
                } else LogHelper.d(TAG, "getPlayingQueueSingle: No result found");
            }
            return mediaItems;
        }

    }


    public List<MediaSessionCompat.QueueItem> getQueue(int queueHint, String uri) {
        List<MediaSessionCompat.QueueItem> items = new ArrayList<>();
        switch (queueHint) {
            case AudioProvider.QueueHint.YOUTUBE_SINGLE_SONG:
                return getPlayingQueueSingle(uri);
            case AudioProvider.QueueHint.YOUTUBE_SONGS:
                return getPlayingQueue(uri);

            default:
                return items;
        }
    }


    public void extractSharedSongQueue(String uri, QueueLoadedCallback callback) {
        String[] splits = uri.split("[|]");
        String videoId = splits[splits.length - 1];
        List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
        Extractor extractor = new Extractor();
        extractor.extract(videoId, new Extractor.Callback() {
            @Override
            public void onSuccess(VideoDetails videoDetails) {
                if (videoDetails != null) {
                    VideoData videoData = videoDetails.getVideoData();
                    Bundle extras = new Bundle();
                    extras.putLong("duration", CommonUtility.stringToMillis(videoData.getLengthSeconds()));
                    MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(new MediaDescriptionCompat.Builder()
                            .setMediaId(videoId)
                            .setTitle(videoData.getTitle())
                            .setSubtitle(videoData.getAuthor())
                            .setDescription(videoData.getShortDescription())
                            .setIconUri(Uri.parse(videoData.getThumbnail().getThumbnails().get(videoData.getThumbnail().getThumbnails().size() - 1).getUrl()))
                            .setExtras(extras)
                            .build(), 0);
                    mediaItems.add(queueItem);
                }
                callback.onLoaded(mediaItems);

            }

            @Override
            public void onError(ExtractionException e) {
                callback.onError(e);
            }
        });


    }







    /*     ---- Generic Playlists Details ----- */

    @Deprecated
    public void getPlaylistsDetails(List<String> playlistIds, PlaylistsLoadedCallback callback) {
        String url = "https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&fields=items(id,snippet(localized,thumbnails(medium,standard)),contentDetails)&maxResults=50&id=" + TextUtils.join(",", playlistIds) + "&key=AIzaSyAdfvLnL55J-5dOwiL_IDF5PydkF3r2jQA";
        JsonObjectRequest request = new JsonObjectRequest(url, null, response -> {
            Gson gson = new Gson();
            PopularPlaylists playlists = gson.fromJson(response.toString(), PopularPlaylists.class);
            callback.onLoaded(playlists.getItems());
        }, error -> {
            callback.onError();
        }) {
            @Override
            protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
                try {
                    Cache.Entry cacheEntry = HttpHeaderParser.parseCacheHeaders(response);
                    if (cacheEntry == null) {
                        cacheEntry = new Cache.Entry();
                    }
                    final long cacheHitButRefreshed = 18 * 60 * 60 * 1000; // in 18 hour cache will be hit, but also refreshed on background
                    final long cacheExpired = 24 * 60 * 60 * 1000; // in 1 days this cache entry expires completely
                    long now = System.currentTimeMillis();
                    final long softExpire = now + cacheHitButRefreshed;
                    final long ttl = now + cacheExpired;
                    cacheEntry.data = response.data;
                    cacheEntry.softTtl = softExpire;
                    cacheEntry.ttl = ttl;
                    String headerValue;
                    headerValue = response.headers.get("Date");
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    headerValue = response.headers.get("Last-Modified");
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue);
                    }
                    cacheEntry.responseHeaders = response.headers;
                    final String jsonString = new String(response.data,
                            HttpHeaderParser.parseCharset(response.headers));
                    return Response.success(new JSONObject(jsonString), cacheEntry);
                } catch (UnsupportedEncodingException | JSONException e) {
                    return Response.error(new ParseError(e));
                }
            }
        };
        requestQueue.add(request);

    }

    //-----------------------------------------------------------
    //               SEARCH
    //-----------------------------------------------------------

    /**
     * @param query
     * @param callback
     */
    public void searchTracks(String query, TracksLoadedCallback callback) {
        executor.execute(() -> {
            try {
                List<com.yash.youtube_extractor.models.YoutubeSong> youtubeSongs = ExtractorHelper.search(query);
                List<YoutubeSong> songs = new ArrayList<>();
                for (com.yash.youtube_extractor.models.YoutubeSong youtubeSong : youtubeSongs) {
                    YoutubeSong song = new YoutubeSong(youtubeSong.getTitle(), youtubeSong.getVideoId(), youtubeSong.getChannelTitle(), youtubeSong.getArtUrlSmall(), youtubeSong.getArtUrlMedium(), youtubeSong.getArtUrlMedium());
                    song.setDurationMillis(youtubeSong.getDurationMillis());
                    songs.add(song);
                }
                loadedTracksMap.put("Search/" + query, songs);
                callback.onLoaded(songs);
            } catch (Exception e) {
                callback.onError(e);
            }
        });
    }


    //-----------------------------------------------------------
    //               FIND
    //-----------------------------------------------------------

    /**
     * New Playlist track provider
     *
     * @param id       Playlist Id
     * @param desc     Description
     * @param callback Callback of success or failed response
     */
    public void getPlaylistTracks(String id, String desc, TracksLoadedCallback callback) {
        LogHelper.d(TAG, "topTracks: Extraction");

        if (!CollectionUtility.isEmpty(loadedTracksMap.get(id))) {
            LogHelper.d(TAG, "Tracks for Playlist %s found in cache!!!", id);
            callback.onLoaded(loadedTracksMap.get(id));
            return;
        }

        executor.execute(() -> {
            try {
                List<YoutubeSong> songs = new ArrayList<>();
                List<com.yash.youtube_extractor.models.YoutubeSong> youtubeSongs = ExtractorHelper.playlistSongs(id).getSongs();
                for (com.yash.youtube_extractor.models.YoutubeSong youtubeSong : youtubeSongs) {
                    YoutubeSong song = new YoutubeSong(youtubeSong.getTitle(), youtubeSong.getVideoId(), youtubeSong.getChannelTitle(), youtubeSong.getArtUrlSmall(), youtubeSong.getArtUrlMedium(), youtubeSong.getArtUrlHigh());
                    song.setDurationMillis(youtubeSong.getDurationMillis());
                    song.setChannelDesc(desc);
                    songs.add(song);
                }
                loadedTracksMap.put(id, songs);
                handler.post(() -> callback.onLoaded(songs));
            } catch (Exception e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }


    public PagedResponse<YoutubeSong> getPlaylistTracks(String id, String desc) {
        LogHelper.d(TAG, "loadPlaylistTracks: [%s]", id);

        try {
            List<YoutubeSong> songs = new ArrayList<>();
            YoutubeResponse youtubeResponse = ExtractorHelper.playlistSongs(id);
            for (com.yash.youtube_extractor.models.YoutubeSong youtubeSong : youtubeResponse.getSongs()) {
                YoutubeSong song = new YoutubeSong(youtubeSong.getTitle(), youtubeSong.getVideoId(), youtubeSong.getChannelTitle(), youtubeSong.getArtUrlSmall(), youtubeSong.getArtUrlMedium(), youtubeSong.getArtUrlHigh());
                song.setDurationMillis(youtubeSong.getDurationMillis());
                song.setChannelDesc(desc);
                songs.add(song);
            }
            LogHelper.d(TAG, "loadPlaylistTracks: Next token : %s", youtubeResponse.getContinuationToken());
            return new PagedResponse<>(songs, null, youtubeResponse.getContinuationToken());
        } catch (Exception e) {
            Log.i(TAG, "loadPlaylistTracks: ", e);
            return new PagedResponse<>(new ArrayList<>(), null, null);
        }
    }

    public PagedResponse<YoutubeSong> getMorePlaylistTracks(String continuationToken, String desc) {
        LogHelper.d(TAG, "loadMorePlaylistTracks: with token [%s]", continuationToken);

        try {
            List<YoutubeSong> songs = new ArrayList<>();
            YoutubeResponse youtubeResponse = ExtractorHelper.continuationResponse(continuationToken, ContinuationType.BROWSE);
            for (com.yash.youtube_extractor.models.YoutubeSong youtubeSong : youtubeResponse.getSongs()) {
                YoutubeSong song = new YoutubeSong(youtubeSong.getTitle(), youtubeSong.getVideoId(), youtubeSong.getChannelTitle(), youtubeSong.getArtUrlSmall(), youtubeSong.getArtUrlMedium(), youtubeSong.getArtUrlHigh());
                song.setDurationMillis(youtubeSong.getDurationMillis());
                song.setChannelDesc(desc);
                songs.add(song);
            }
            LogHelper.d(TAG, "loadMorePlaylistTracks: Next token : %s", youtubeResponse.getContinuationToken());
            return new PagedResponse<>(songs, null, youtubeResponse.getContinuationToken());
        } catch (Exception e) {
            Log.i(TAG, "loadMorePlaylistTracks: ", e);
            return new PagedResponse<>(new ArrayList<>(), null, null);
        }
    }

    public void getChannelPlaylists(String channelId, PlaylistLoadedCallback callback) {
        LogHelper.d(TAG, "Channel Playlists of id : %s", channelId);

        if (!CollectionUtility.isEmpty(loadedPlaylistsMap.get(channelId))) {
            LogHelper.d(TAG, "Playlists for Channel %s found in cache!!!", channelId);
            callback.onLoaded(loadedPlaylistsMap.get(channelId));
            return;
        }

        executor.execute(() -> {
            try {
                List<Pair<String, List<com.yash.youtube_extractor.models.YoutubePlaylist>>> playlists = new ArrayList<>();
                Map<String, List<com.yash.youtube_extractor.models.YoutubePlaylist>> playlistsByCategory = ExtractorHelper.channelPlaylists(channelId);
                for (Map.Entry<String, List<com.yash.youtube_extractor.models.YoutubePlaylist>> entry : playlistsByCategory.entrySet()) {
                    if (CollectionUtility.isEmpty(entry.getValue()))
                        continue;
                    playlists.add(Pair.create(entry.getKey(), entry.getValue()));
                }
                loadedPlaylistsMap.put(channelId, playlists);
                handler.post(() -> callback.onLoaded(playlists));
            } catch (Exception e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }


}

