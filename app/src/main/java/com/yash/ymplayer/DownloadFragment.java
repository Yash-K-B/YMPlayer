package com.yash.ymplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.databinding.DownloadItemBinding;
import com.yash.ymplayer.databinding.FragmentDownloadBinding;
import com.yash.ymplayer.models.DownloadFile;
import com.yash.ymplayer.util.ConverterUtil;
import com.yash.ymplayer.util.Keys;
import com.yash.youtube_extractor.Extractor;
import com.yash.youtube_extractor.exceptions.ExtractionException;
import com.yash.youtube_extractor.models.VideoDetails;

import org.jetbrains.annotations.NotNull;

import java.security.Key;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadFragment extends Fragment {
    private static final String TAG = "DownloadFragment";

    FragmentDownloadBinding downloadBinding;
    Context context;
    SharedPreferences preferences;
    List<DownloadFile> files = new ArrayList<>();
    Gson gson;

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gson = new Gson();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        preferences = context.getSharedPreferences(Keys.SHARED_PREFERENCES.DOWNLOADS, Context.MODE_PRIVATE);
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        downloadBinding = FragmentDownloadBinding.inflate(inflater, container, false);
        return downloadBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ((ActivityActionProvider) context).setCustomToolbar(null, "Downloads");
        int numFiles = preferences.getInt(Keys.PREFERENCE_KEYS.TOTAL_DOWNLOADS, 0);
        for (int i = numFiles - 1; i >= 0; i--) {
            String fileJson = preferences.getString(Keys.PREFERENCE_KEYS.DOWNLOADS + i, "{}");
            DownloadFile file = gson.fromJson(fileJson, DownloadFile.class);
            files.add(file);
        }
        DownloadFileAdapter adapter = new DownloadFileAdapter(context, files, new DownloadFileAdapter.OnClickListener() {
            @Override
            public void onClick(DownloadFile file) {
                String mediaId = file.uri.substring(file.uri.lastIndexOf('/'));
                ((ActivityActionProvider) context).interactWithMediaSession(mediaController -> {
                    Bundle extra = new Bundle();
                    extra.putBoolean(Keys.PLAY_SINGLE,true);
                    mediaController.getTransportControls().playFromMediaId(mediaId, extra);
                });
            }
        });
        downloadBinding.downloads.setLayoutManager(new LinearLayoutManager(context));
        downloadBinding.downloads.setAdapter(adapter);

    }

    public static class DownloadFileAdapter extends RecyclerView.Adapter<DownloadFileAdapter.DownloadFileViewHolder> {
        Context context;
        List<DownloadFile> files;
        OnClickListener listener;

        public DownloadFileAdapter(Context context, List<DownloadFile> files, OnClickListener listener) {
            this.context = context;
            this.files = files;
            this.listener = listener;
        }

        @NonNull
        @Override
        public DownloadFileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            DownloadItemBinding binding = DownloadItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new DownloadFileViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull DownloadFileViewHolder holder, int position) {
            holder.bind(files.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        class DownloadFileViewHolder extends RecyclerView.ViewHolder {
            DownloadItemBinding binding;

            public DownloadFileViewHolder(DownloadItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(DownloadFile file, OnClickListener listener) {
                Glide.with(context).load(file.fileImageUrl).into(binding.fileArt);
                binding.fileArt.setClipToOutline(true);
                binding.fileName.setText(file.fileName);
                binding.subName.setText(String.format("by %s", file.fileSubText));
                binding.fileSize.setText(ConverterUtil.getFormattedFileSize(file.fileLength));
                binding.fileFormat.setText(file.extension.toUpperCase());
                binding.bitrate.setText(String.format(Locale.US,"%d kbps", file.bitrate));
                itemView.setOnClickListener(v -> listener.onClick(file));
            }


        }

        public interface OnClickListener {
            void onClick(DownloadFile file);
        }

    }
}