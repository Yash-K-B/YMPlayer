package com.yash.ymplayer.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.cache.impl.LoadedTracksCache;
import com.yash.ymplayer.constant.Constants;
import com.yash.ymplayer.interfaces.AudioProvider;
import com.yash.ymplayer.ui.youtube.livepage.PagedResponse;
import com.yash.ymplayer.util.MediaItemHelperUtility;
import com.yash.ymplayer.util.YoutubeSong;
import com.yash.youtube_extractor.Extractor;
import com.yash.youtube_extractor.ExtractorHelper;
import com.yash.youtube_extractor.constants.ContinuationType;
import com.yash.youtube_extractor.exceptions.ExtractionException;
import com.yash.youtube_extractor.models.VideoData;
import com.yash.youtube_extractor.models.VideoDetails;
import com.yash.youtube_extractor.models.YoutubeChannelInfo;
import com.yash.youtube_extractor.models.YoutubePlaylist;
import com.yash.youtube_extractor.models.YoutubeResponse;
import com.yash.youtube_extractor.service.ChannelExtractor;
import com.yash.youtube_extractor.utility.CollectionUtility;
import com.yash.youtube_extractor.utility.CommonUtility;
import com.yash.youtube_extractor.utility.RequestUtility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OnlineYoutubeRepository {
    private static final String TAG = "OnlineYoutubeRepository";
    Map<String, TopTracksYoutubeSongsCache> topTracksCache;
    Map<String, List<YoutubeSong>> loadedTracksMap;
    Map<String, Map<String, List<YoutubePlaylist>>> loadedPlaylistsMap;
    Context context;
    static OnlineYoutubeRepository instance;
    RequestQueue requestQueue;
    RequestedTracksLoadedCallback callback;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    private final LoadedTracksCache loadedTracksCache;

    OnlineYoutubeRepository(Context context) {
        this.context = context.getApplicationContext();
        this.requestQueue = Volley.newRequestQueue(context);
        this.topTracksCache = new LinkedHashMap<>();
        this.loadedTracksMap = new HashMap<>();
        this.loadedPlaylistsMap = new HashMap<>();
        this.loadedTracksCache = new LoadedTracksCache();
    }

    //singleton
    public static OnlineYoutubeRepository getInstance(Context context) {
        if (instance == null)
            instance = new OnlineYoutubeRepository(context);
        return instance;
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


    public interface QueueLoadedCallback {
        void onLoaded(List<MediaSessionCompat.QueueItem> queueItems);

        default void additionalDetail(VideoDetails videoDetails) {}

        default <E extends Exception> void onError(E e) {};
    }

    public interface WatchNextQueueLoadedCallback {
        void onLoaded(List<MediaSessionCompat.QueueItem> queueItems, String continuationToken, String tag);

        default <E extends Exception> void onError(E e) {
            LogHelper.d(TAG, "onError: Error while fetching watch next queue");
        };
    }

    public interface TracksLoadedCallback {
        void onLoaded(List<YoutubeSong> songs);

        <E extends Exception> void onError(E e);
    }

    public interface PlaylistLoadedCallback {
        void onLoaded(Map<String, List<YoutubePlaylist>> playlistsByCategory);

        <E extends Exception> void onError(E e);
    }


    /**
     * Build playing queue of uri
     *
     * @param uri Uri
     * @return List of queue items
     */
    public List<MediaSessionCompat.QueueItem> getPlayingQueue(String uri) {
        String id = uri.split("[|]")[0];
        if ("TOP_TRACKS".equals(id)) {
            id = Constants.DEFAULT_PLAYLIST;
        }

        List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
        long queueId = 0L;
        if (loadedTracksCache.contain(id)) {
            for (YoutubeSong song : loadedTracksCache.get(id).getItems()) {
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
        else LogHelper.d(TAG, "getPlayingQueue: No result found");
        return mediaItems;
    }

    /**
     * Build single playing queue of uri
     *
     * @param uri Uri
     * @return List of queue items
     */
    public List<MediaSessionCompat.QueueItem> getPlayingQueueSingle(String uri) {
        String id = uri.split("[|]")[0];
        String videoId = uri.split("[|]")[1];
        if ("TOP_TRACKS".equals(id)) {
            id = Constants.DEFAULT_PLAYLIST;
        }

        List<MediaSessionCompat.QueueItem> mediaItems = new ArrayList<>();
        long queueId = 0L;
        if (loadedTracksCache.contain(id)) {
            for (YoutubeSong song : loadedTracksCache.get(id).getItems()) {
                if(!song.getVideoId().equals(videoId))
                    continue;
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
        else LogHelper.d(TAG, "getPlayingQueueSingle: No result found");
        return mediaItems;
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
                    callback.additionalDetail(videoDetails);
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

    public void extractSharedWatchNextQueue(String uri, String continuationToken, String tag, WatchNextQueueLoadedCallback callback) {
        executor.submit(() -> {
            String[] splits = uri.split("[|]");
            String videoId = splits[splits.length - 1];
            try {
                YoutubeResponse youtubeResponse = continuationToken == null? ExtractorHelper.watchNext(videoId): ExtractorHelper.watchNextContinuation(videoId, continuationToken);
                callback.onLoaded(MediaItemHelperUtility.toQueueItems(youtubeResponse.getSongs()), youtubeResponse.getContinuationToken(), tag);
            } catch (ExtractionException e) {
                callback.onError(e);
            }
        });
    }

    //----------------------------------------------------------
    //               WATCH NEXT
    //----------------------------------------------------------
    public PagedResponse<YoutubeSong> watchNextTracks(String videoId) {
        LogHelper.d(TAG, "watchNextTracks: with video id [%s]", videoId);

        try {
            YoutubeResponse youtubeResponse = ExtractorHelper.watchNext(videoId);
            LogHelper.d(TAG, "watchNextTracks:\n\tTotal Tracks: %s\n\tNext token : %s", youtubeResponse.getSongs().size(), youtubeResponse.getContinuationToken());
            return PagedResponse.from(youtubeResponse);
        } catch (Exception e) {
            Log.i(TAG, "watchNextTracks: ", e);
            return PagedResponse.empty();
        }
    }

    public PagedResponse<YoutubeSong> watchNextMoreTracks(String videoId, String continuationToken) {
        LogHelper.d(TAG, "watchNextMoreTracks: with video id [%s - %s]", videoId, continuationToken);

        try {
            YoutubeResponse youtubeResponse = ExtractorHelper.watchNextContinuation(videoId, continuationToken);
            LogHelper.d(TAG, "watchNextMoreTracks:\n\tTotal Tracks: %s\n\tNext token : %s", youtubeResponse.getSongs().size(), youtubeResponse.getContinuationToken());
            return PagedResponse.from(youtubeResponse);
        } catch (Exception e) {
            Log.i(TAG, "watchNextMoreTracks: ", e);
            return PagedResponse.empty();
        }
    }



    //-----------------------------------------------------------
    //               SEARCH
    //-----------------------------------------------------------

    /**
     * Search a string from youtube
     *
     * @param query    Query text
     */
    public PagedResponse<YoutubeSong> searchTracks(String query) {
        LogHelper.d(TAG, "searchTracks: with query [%s]", query);
        String key = Constants.PREFIX_SEARCH + query;

        try {
            YoutubeResponse youtubeResponse = ExtractorHelper.search(query);
            LogHelper.d(TAG, "searchTracks:\n\tTotal Tracks: %s\n\tNext token : %s", youtubeResponse.getSongs().size(), youtubeResponse.getContinuationToken());
            return PagedResponse.from(youtubeResponse);
        } catch (Exception e) {
            Log.i(TAG, "searchTracks: ", e);
            return PagedResponse.empty();
        }
    }

    public PagedResponse<YoutubeSong> searchMoreTracks(String query, String continuationToken) {
        LogHelper.d(TAG, "searchMoreTracks: with query [%s - %s]", query, continuationToken);
        String key = Constants.PREFIX_SEARCH + query + "-" + continuationToken;

        try {
            YoutubeResponse youtubeResponse = ExtractorHelper.searchContinuation(query, continuationToken);
            LogHelper.d(TAG, "searchMoreTracks:\n\tTotal Tracks: %s\n\tNext token : %s", youtubeResponse.getSongs().size(), youtubeResponse.getContinuationToken());
            return PagedResponse.from(youtubeResponse);
        } catch (Exception e) {
            Log.i(TAG, "searchMoreTracks: ", e);
            return PagedResponse.empty();
        }
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
        LogHelper.d(TAG, "getPlaylistTracks: ");
        executor.execute(() -> {
            try {
                PagedResponse<YoutubeSong> tracks = getPlaylistTracks(id, desc);
                handler.post(() -> callback.onLoaded(tracks.getItems()));
            } catch (Exception e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }


    /**
     * Paged Response of Youtube Playlist Tracks
     *
     * @param playlistId Playlist ID
     * @param desc       Description
     * @return Paged Response
     */
    public PagedResponse<YoutubeSong> getPlaylistTracks(String playlistId, String desc) {
        LogHelper.d(TAG, "loadPlaylistTracks: [%s]", playlistId);

        if(loadedTracksCache.contain(playlistId)) {
            LogHelper.d(TAG, "Tracks for %s found in Cache", playlistId);
            return loadedTracksCache.get(playlistId);
        }

        try {
            YoutubeResponse youtubeResponse = ExtractorHelper.playlistSongs(playlistId);
            LogHelper.d(TAG, "loadPlaylistTracks:\n\tTotal Tracks: %s\n\tNext token : %s", youtubeResponse.getSongs().size(), youtubeResponse.getContinuationToken());
            PagedResponse<YoutubeSong> pagedResponse = PagedResponse.from(youtubeResponse);
            loadedTracksCache.put(playlistId, pagedResponse);
            return pagedResponse;
        } catch (Exception e) {
            Log.i(TAG, "loadPlaylistTracks: ", e);
            return PagedResponse.empty();
        }
    }

    /**
     * Paged Response of Youtube playlist tracks continuation
     *
     * @param playlistId        Playlist ID
     * @param continuationToken Continuation token
     * @param desc              Description
     * @return Paged Response
     */
    public PagedResponse<YoutubeSong> getMorePlaylistTracks(String playlistId, String continuationToken, String desc) {
        LogHelper.d(TAG, "loadMorePlaylistTracks: with  playlistId [%s] and token [%s]", playlistId, continuationToken);

        String key = playlistId + "-" + continuationToken;
        if(loadedTracksCache.contain(key)) {
            LogHelper.d(TAG, "Tracks for %s found in Cache", key);
            return loadedTracksCache.get(key);
        }
        try {
            YoutubeResponse youtubeResponse = ExtractorHelper.continuationResponse(continuationToken, ContinuationType.BROWSE);
            LogHelper.d(TAG, "loadMorePlaylistTracks:\n\tTotal Tracks: %s\n\tNext token : %s", youtubeResponse.getSongs().size(), youtubeResponse.getContinuationToken());
            PagedResponse<YoutubeSong> pagedResponse = PagedResponse.from(youtubeResponse);
            loadedTracksCache.put(key, pagedResponse);
            return pagedResponse;
        } catch (Exception e) {
            Log.i(TAG, "loadMorePlaylistTracks: ", e);
            return PagedResponse.empty();
        }
    }

    /**
     * Playlists of Channel
     *
     * @param channelId - Channel ID
     * @param callback - Callback to receive result
     */
    public void getChannelPlaylists(String channelId, PlaylistLoadedCallback callback) {
        LogHelper.d(TAG, "Channel Playlists of id : %s", channelId);

//        if (!CollectionUtility.isEmpty(loadedPlaylistsMap.get(channelId))) {
//            LogHelper.d(TAG, "Playlists for Channel %s found in cache!!!", channelId);
//            callback.onLoaded(loadedPlaylistsMap.get(channelId));
//            return;
//        }

        executor.execute(() -> {
            try {
                Map<String, List<YoutubePlaylist>> playlists = new LinkedHashMap<>();
                YoutubeChannelInfo youtubeChannelInfo = ChannelExtractor.INSTANCE.channelInfoV2(channelId);
                updateUserYoutubeInfo(youtubeChannelInfo);
                for (Map.Entry<String, List<YoutubePlaylist>> entry : youtubeChannelInfo.getPlaylistsByCategory().entrySet()) {
                    if (CollectionUtility.isEmpty(entry.getValue()))
                        continue;
                    playlists.put(entry.getKey(), entry.getValue());
                }

                loadedPlaylistsMap.put(channelId, playlists);
                handler.post(() -> callback.onLoaded(playlists));
            } catch (Exception e) {
                handler.post(() -> callback.onError(e));
            }
        });
    }

    void updateUserYoutubeInfo(YoutubeChannelInfo channelInfo){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("VISITOR_DATA_KEY", channelInfo.getVisitorData());
        editor.putString("CLIENT_VERSION_KEY", channelInfo.getClientVersion());
        editor.putString("CLIENT_API_KEY", channelInfo.getApiKey());
        editor.apply();
        RequestUtility.updateSettings(preferences);
    }

}

