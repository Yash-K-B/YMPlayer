package com.yash.ymplayer.repository;

public class OnlineRepository {
//    private static final String TAG = "debug";
//    Context context;
//    static OnlineRepository instance;
//    final String BASE_URL = "https://api.spotify.com/v1";
//    MutableLiveData<List<MediaBrowserCompat.MediaItem>> spotifyArtists = new MutableLiveData<>();
//    SharedPreferences preferences;
//    RequestQueue requestQueue;
//
//    public static OnlineRepository getInstance(Context context) {
//        if (instance == null)
//            instance = new OnlineRepository(context.getApplicationContext());
//        return instance;
//    }
//
//    public OnlineRepository(Context context) {
//        this.context = context;
//        requestQueue = Volley.newRequestQueue(context);
//        preferences = context.getSharedPreferences(Keys.SHARED_PREFERENCES.SPOTIFY, Context.MODE_PRIVATE);
//    }
//
//
//    public LiveData<List<MediaBrowserCompat.MediaItem>> getSpotifySongs() {
//        fetchArtistsData();
//        return spotifyArtists;
//    }
//
//    void fetchArtistsData() {
//
//        String ids = "/artists?ids=4YRxDV8wJFPHPTeXepOstw,0Xbdgzdm7k9BJ5gUgmAkpy,0oOet2f43PA68X5RxKobEy,1dVygo6tRFXC8CSWURQJq2,5f4QpKfy7ptCHwTqspnSJI,4f7KfxeHq9BiylGmyXepGt"+
//                ",0y59o4v8uw5crbN9M3JiL1,1uNFoZAHBGtllmzznpCI3s,1mYsTxnqsietFxj1OgoGbG,1sVmXkzX2ukc6QvasrDBES,0T1CMVkqffHlqEk4BcAph1";
//        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, BASE_URL + ids, null, response -> {
//            Gson gson = new Gson();
//            SpotifyArtistsModel spotifyArtistsModel = gson.fromJson(response.toString(), SpotifyArtistsModel.class);
//            List<MediaBrowserCompat.MediaItem> mediaItems = mapToMediaItems(spotifyArtistsModel);
//            this.spotifyArtists.setValue(mediaItems);
//        }, error -> {
//
//        }) {
//            @Override
//            public Map<String, String> getHeaders() throws AuthFailureError {
//                Map<String, String> headers = new HashMap<>();
//                String token = preferences.getString(Keys.PREFERENCE_KEYS.TOKEN, null);
//                Log.d(TAG, "getHeaders: token:" + token);
//                String auth = " Bearer " + token;
//                headers.put("Authorization", auth);
//                return headers;
//            }
//        };
//        requestQueue.add(jsonObjectRequest);
//    }
//
//    private List<MediaBrowserCompat.MediaItem> mapToMediaItems(SpotifyArtistsModel spotifyArtists) {
//        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//        for (SpotifyArtist spotifyArtist : spotifyArtists.getArtists()) {
//            MediaItem item = new MediaItem(new MediaDescriptionCompat.Builder()
//                    .setMediaId(spotifyArtist.getUri())
//                    .setTitle(spotifyArtist.getName())
//                    .setSubtitle(spotifyArtist.getType())
//                    .setIconUri(Uri.parse(spotifyArtist.getImages().get(spotifyArtist.getImages().size() - 1).getUrl()))
//                    .build(), MediaItem.FLAG_BROWSABLE);
//            mediaItems.add(item);
//        }
//        return mediaItems;
//    }
//

}
