package com.yash.ymplayer.util;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.yash.ymplayer.download.manager.DownloadService;
import com.yash.ymplayer.R;
import com.yash.ymplayer.constant.Constants;
import com.yash.ymplayer.interfaces.Keys;
import com.yash.ymplayer.interfaces.MediaIDProvider;
import com.yash.ymplayer.ui.custom.ConnectionAwareFragment;

import java.util.regex.Pattern;

public class CommonUtil {
    private static final String TAG = "CommonUtil";

    public static Pattern deviceUriPattern = Pattern.compile(Constants.DEVICE_URI_PREFIX_REGEX);
    public static Pattern offlineAudioPattern = Pattern.compile("[0-9]+");


    public static String extractId(String mediaId) {
        if (mediaId == null)
            return null;
        String[] splits = mediaId.split("[/|]");
        return splits[splits.length - 1];
    }

    public static Integer extractIntegerId(String mediaId) {
        if (mediaId == null)
            return null;
        String[] splits = mediaId.split("[/|]");
        return Integer.parseInt(splits[splits.length - 1]);
    }

    public static boolean isYoutubeSong(String mediaId) {
        if (mediaId == null)
            return false;
        if (mediaId.startsWith("content://") || mediaId.startsWith("file://"))
            return false;
        String[] splits = mediaId.split("[/|]");
        return !offlineAudioPattern.matcher(splits[splits.length - 1]).matches();
    }

    public static PopupMenu buildYoutubeDownloadPopup(Context context, View view, MediaIDProvider provider) {
        PopupMenu menu = new PopupMenu(context, view);
        menu.inflate(R.menu.youtube_download_menu);
        menu.setOnMenuItemClickListener(item -> {
            Intent downloadIntent;
            switch (item.getItemId()) {
                case R.id.download128kbps:
                    downloadIntent = new Intent(context, DownloadService.class);
                    downloadIntent.putExtra(Keys.VIDEO_ID, provider.getMediaId());
                    downloadIntent.putExtra(Keys.EXTRA_DOWNLOAD_QUALITY, 128);
                    context.startService(downloadIntent);
                    return true;
                case R.id.download192kbps:
                    downloadIntent = new Intent(context, DownloadService.class);
                    downloadIntent.putExtra(Keys.VIDEO_ID, provider.getMediaId());
                    downloadIntent.putExtra(Keys.EXTRA_DOWNLOAD_QUALITY, 192);
                    context.startService(downloadIntent);
                    return true;
                case R.id.download320kbps:
                    downloadIntent = new Intent(context, DownloadService.class);
                    downloadIntent.putExtra(Keys.VIDEO_ID, provider.getMediaId());
                    downloadIntent.putExtra(Keys.EXTRA_DOWNLOAD_QUALITY, 320);
                    context.startService(downloadIntent);
                    return true;

                default:
                    return false;
            }
        });
        return menu;
    }

    public static View.OnClickListener buildShareSong(Context context, MediaIDProvider provider) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String uriOrId = provider.getMediaId();
                String[] parts = uriOrId.split("[|/]");
                if (offlineAudioPattern.matcher(parts[parts.length - 1]).matches()) {
                    long mediaId = Long.parseLong(parts[parts.length - 1]);
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, mediaId);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("audio/*");
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(Intent.createChooser(intent, "Share Song via"));
                } else if (deviceUriPattern.matcher(uriOrId).matches()) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("audio/*");
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(uriOrId));
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(Intent.createChooser(intent, "Share Song via"));
                } else {
                    String baseUrl = "https://youtu.be/";
                    Intent shareIntent = new Intent(Intent.ACTION_VIEW);
                    shareIntent.setData(Uri.parse(baseUrl + uriOrId));
                    context.startActivity(shareIntent);
                }

            }
        };
    }

    private static final MediaMetadataRetriever retriever = new MediaMetadataRetriever();

    public static Bitmap getEmbeddedPicture(Context context, String uri) {
        return getEmbeddedPicture(context, Uri.parse(uri));
    }
    public static Bitmap getEmbeddedPicture(Context context, Uri uri) {
        retriever.setDataSource(context, uri);
        byte[] embeddedPicture = retriever.getEmbeddedPicture();
        embeddedPicture = embeddedPicture == null ? new byte[]{} : embeddedPicture;
        return BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.length);
    }

    public static Bitmap getEmbeddedPictureOrDefault(Context context, Uri uri, Drawable defaultPicture) {
        Bitmap embeddedPicture = getEmbeddedPicture(context, uri);
        return embeddedPicture != null? embeddedPicture: ((BitmapDrawable) defaultPicture).getBitmap();
    }

    public static ConnectionAwareFragment getConnectionAwareFragment(Fragment fragment) {
        while ((fragment = fragment.getParentFragment()) != null) {
            if(fragment instanceof ConnectionAwareFragment) {
                return  ((ConnectionAwareFragment) fragment);
            }
        }
        throw new ActivityNotFoundException("No connection aware fragment available");
    }
}
