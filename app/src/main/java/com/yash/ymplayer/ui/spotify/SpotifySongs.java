package com.yash.ymplayer.ui.spotify;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.PreferenceManager;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.yash.ymplayer.ActivityActionProvider;
import com.yash.ymplayer.MainActivity;
import com.yash.ymplayer.databinding.FragmentSpotifySongsBinding;
import com.yash.ymplayer.util.Keys;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.spotify.sdk.android.auth.LoginActivity.REQUEST_CODE;

public class SpotifySongs extends Fragment {
    private static final String TAG = "debug";
    private static final String CLIENT_ID = "e3ed7f2ee39942bbbde56c6029d7fbea";
    private static final String CLIENT_SECRET_ID = "dc24e3458d544f95a6e121bed1ba7760";
    public static final String REDIRECT_URI = "spotifysongs://callback";
    public static final int REQUEST_CODE = 1137;
    private static final String SCOPES = "user-read-recently-played,user-library-modify,user-read-email,user-read-private";
    public static final String[] TABS = {"TRACKS", "ALBUMS", "ARTISTS", "PLAYLISTS"};
    FragmentSpotifySongsBinding spotifySongsBinding;
    FragmentActivity activity;
    Context context;
    SharedPreferences preferences;
    RequestQueue requestQueue;
    ConnectivityManager connectivityManager;
    Handler handler = new Handler();

    public SpotifySongs() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        spotifySongsBinding = FragmentSpotifySongsBinding.inflate(inflater, container, false);
        return spotifySongsBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        activity = getActivity();
        context = getContext();
        requestQueue = Volley.newRequestQueue(context);
        ((ActivityActionProvider) activity).setCustomToolbar(spotifySongsBinding.spotifyToolbar, "Spotify Library");
        preferences = context.getSharedPreferences(Keys.SHARED_PREFERENCES.SPOTIFY, Context.MODE_PRIVATE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_VPN);
        connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;
        connectivityManager.registerNetworkCallback(builder.build(), callback);
        Log.d(TAG, "SpotifySongs : onActivityCreated:");
        checkNetworkAccess();
    }

    void checkNetworkAccess() {

        if (connectivityManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (networkCapabilities == null) return;
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                Snackbar.make(spotifySongsBinding.getRoot(), "Internet Available", Snackbar.LENGTH_LONG).show();
                handler.post(()->spotifySongsBinding.loadingHint.setVisibility(View.VISIBLE));
                if (preferences.getString(Keys.PREFERENCE_KEYS.TOKEN, null) == null)
                    authenticateSpotify();
                else handler.post(this::refresh);
            } else {
                Snackbar.make(spotifySongsBinding.getRoot(), "No Network Access", Snackbar.LENGTH_LONG).show();
            }
        } else {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isConnected()) {
                Snackbar.make(spotifySongsBinding.getRoot(), "Internet Available", Snackbar.LENGTH_LONG).show();
                handler.post(()->spotifySongsBinding.loadingHint.setVisibility(View.VISIBLE));
                if (preferences.getString(Keys.PREFERENCE_KEYS.TOKEN, null) == null)
                    authenticateSpotify();
                else handler.post(this::refresh);
            } else {
                Snackbar.make(spotifySongsBinding.getRoot(), "No Network Access", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Main stuffs are done here
     */
    public void refresh() {
        Log.d(TAG, "refresh: Spotify Songs");
        String url = "https://api.spotify.com/v1/me";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d(TAG, "onResponse: Name:" + response.getString("display_name"));
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    editor.putString("user_name", response.getString("display_name"));
                    editor.apply();
                    ((ActivityActionProvider) getActivity()).setUserName(response.getString("display_name"));
                    handler.post(SpotifySongs.this::bindUi);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: ");
                error.printStackTrace();
                if (error.networkResponse.statusCode == 401)
                    authenticateSpotify();
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = preferences.getString(Keys.PREFERENCE_KEYS.TOKEN, null);
                Log.d(TAG, "getHeaders: token:" + token);
                String auth = " Bearer " + token;
                headers.put("Authorization", auth);
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);
    }

    void bindUi(){
        spotifySongsBinding.loadingHint.setVisibility(View.INVISIBLE);
        SpotifyViewPagerAdapter spotifyViewPagerAdapter = new SpotifyViewPagerAdapter(getChildFragmentManager(), activity.getLifecycle());
        spotifySongsBinding.spotifyViewpager.setAdapter(spotifyViewPagerAdapter);
        new TabLayoutMediator(spotifySongsBinding.tabs, spotifySongsBinding.spotifyViewpager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                Log.d(TAG, "onConfigureTab: pos: "+position+" name: "+TABS[position]);
                tab.setText(TABS[position]);
            }
        }).attach();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    void authenticateSpotify() {
        AuthorizationRequest.Builder builder;
        builder = new AuthorizationRequest.Builder(CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI);
        builder.setScopes(new String[]{SCOPES});
        AuthorizationRequest request = builder.build();
        AuthorizationClient.openLoginActivity(activity, REQUEST_CODE, request);
    }

    ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            Log.d(TAG, "onAvailable: ");
            checkNetworkAccess();
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            Log.d(TAG, "onLost: ");
            checkNetworkAccess();
        }

    };
}