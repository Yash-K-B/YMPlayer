package com.yash.ymplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.databinding.DownloadItemBinding;
import com.yash.ymplayer.databinding.FragmentDownloadBinding;
import com.yash.ymplayer.download.manager.DownloadRepository;
import com.yash.ymplayer.download.manager.DownloadService;
import com.yash.ymplayer.download.manager.DownloadTask;
import com.yash.ymplayer.download.manager.Downloader;
import com.yash.ymplayer.download.manager.constants.DownloadStatus;
import com.yash.ymplayer.download.manager.models.Download;
import com.yash.ymplayer.repository.Repository;
import com.yash.ymplayer.util.ConverterUtil;
import com.yash.ymplayer.util.Keys;
import com.yash.ymplayer.util.StorageXI;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadFragment extends Fragment {
    private static final String TAG = "DownloadFragment";

    FragmentDownloadBinding downloadBinding;
    Context context;
    SharedPreferences preferences;
    List<Download> files = new ArrayList<>();
    DownloadFileAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public DownloadFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        downloadBinding = FragmentDownloadBinding.inflate(inflater, container, false);
        return downloadBinding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refresh();
        downloadBinding.downloadsRefresh.setOnRefreshListener(() -> {
            downloadBinding.downloadsRefresh.setRefreshing(true);
            refresh();
            adapter.notifyDataSetChanged();
            downloadBinding.downloadsRefresh.setRefreshing(false);
        });
        adapter = new DownloadFileAdapter(context, getViewLifecycleOwner(), files, onClickListener, launcher);
        downloadBinding.downloads.setLayoutManager(new LinearLayoutManager(context));
        downloadBinding.downloads.setAdapter(adapter);
    }

    public void refresh() {
        files.clear();
        ((ActivityActionProvider) context).setCustomToolbar(null, "Downloads");
        List<Download> downloads = DownloadRepository.getInstance(context).getDownloadDao().findAll();
        files.addAll(downloads);
    }

    private final DownloadFileAdapter.OnClickListener onClickListener = new DownloadFileAdapter.OnClickListener() {
        @Override
        public void onClick(Download file, int adapterPosition) {
            if (file.getStatus() == DownloadStatus.DOWNLOADING || file.getStatus() == DownloadStatus.PROCESSING) {
                Downloader instance = Downloader.getInstance();
                if (instance != null)
                    instance.pause(file.getId(), file.getVideoId());
            } else if (file.getStatus() == DownloadStatus.PAUSED || file.getStatus() == DownloadStatus.FAILED) {
                LogHelper.d(TAG, "onClick: Download id - %s", file.getId());
                Intent downloadIntent = new Intent(context, DownloadService.class);
                downloadIntent.putExtra(Keys.DownloadManager.EXTRA_VIDEO_ID, file.getVideoId());
                downloadIntent.putExtra(Keys.DownloadManager.EXTRA_BITRATE, file.getBitrate());
                downloadIntent.putExtra(Keys.DownloadManager.DOWNLOAD_ID, file.getId());
                context.startService(downloadIntent);
                handler.postDelayed(() -> adapter.notifyItemChanged(adapterPosition), 500);
            } else {
                if (file.getUri() == null) {
                    Toast.makeText(context, "Corrupted file", Toast.LENGTH_SHORT).show();
                    return;
                }
                LogHelper.d(TAG, "onClick: URI : %s", file.getUri());
                String mediaId = file.getUri().substring(file.getUri().lastIndexOf('/') + 1);
                ((ActivityActionProvider) context).interactWithMediaSession(mediaController -> {
                    Bundle extra = new Bundle();
                    extra.putBoolean(Keys.PLAY_SINGLE, true);
                    mediaController.getTransportControls().playFromMediaId(mediaId, extra);
                });
            }

        }

    };

    public static class DownloadFileAdapter extends RecyclerView.Adapter<DownloadFileAdapter.DownloadFileViewHolder> {
        Context context;
        List<Download> files;
        OnClickListener listener;
        LifecycleOwner lifecycleOwner;
        ActivityResultLauncher<IntentSenderRequest> launcher;

        public DownloadFileAdapter(Context context, LifecycleOwner lifecycleOwner, List<Download> files, OnClickListener listener, ActivityResultLauncher<IntentSenderRequest> launcher) {
            this.context = context;
            this.files = files;
            this.listener = listener;
            this.lifecycleOwner = lifecycleOwner;
            this.launcher = launcher;
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
        public void onViewRecycled(@NonNull DownloadFileViewHolder holder) {
            holder.destroy();
        }

        @Override
        public int getItemCount() {
            return files.size();
        }

        class DownloadFileViewHolder extends RecyclerView.ViewHolder {
            DownloadItemBinding binding;
            Download file;
            DownloadTask task = null;

            public DownloadFileViewHolder(DownloadItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            public void bind(Download file, OnClickListener listener) {
                this.file = file;
                Glide.with(context).load(file.getFileImageUrl()).into(binding.fileArt);
                binding.fileArt.setClipToOutline(true);
                binding.fileName.setText(file.getFileName());
                binding.subName.setText(String.format("by %s", file.getFileSubText()));
                binding.fileSize.setText(ConverterUtil.getFormattedFileSize(file.getFileLength()));
                binding.fileFormat.setText(file.getExtension().toUpperCase());
                binding.bitrate.setText(String.format(Locale.US, "%d kbps", file.getBitrate()));
                if (file.getStatus() != DownloadStatus.DOWNLOADED)
                    binding.status.setText(file.getStatus().getValue());
                itemView.setOnClickListener(v -> listener.onClick(file, getAdapterPosition()));
                itemView.setOnLongClickListener(v -> showLongPressMenu());
                attachInprogressTask(file);

            }


            private boolean showLongPressMenu() {
                PopupMenu menu = new PopupMenu(context, binding.fileName, Gravity.END);
                menu.inflate(R.menu.download_item_context_menu);
                menu.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.remove:
                            DownloadRepository.getInstance(context).getDownloadDao().delete(file.getId());
                            files.remove(getAdapterPosition());
                            notifyItemRemoved(getAdapterPosition());
                            return true;

                        case R.id.deleteFromStorage:
                            if (file.getUri() != null) {
                                String[] parts = file.getUri().split("[/|]");
                                long mediaId = Long.parseLong(parts[parts.length - 1]);
                                StorageXI.getInstance().with(context).delete(launcher, mediaId);
                            }
                            DownloadRepository.getInstance(context).getDownloadDao().delete(file.getId());
                            files.remove(getAdapterPosition());
                            notifyItemRemoved(getAdapterPosition());
                            return true;
                    }
                    return true;
                });
                menu.show();
                return true;
            }

            public void attachInprogressTask(Download file) {
                Downloader instance = Downloader.getInstance();
                DownloadTask task;
                if (instance != null && (task = instance.findTask(file.getId())) != null) {
                    task.getStatus().observe(lifecycleOwner, statusChangeObserver);
                    task.getProgress().observe(lifecycleOwner, progressChangeObserver);
                } else if (file.getStatus() == DownloadStatus.DOWNLOADING || file.getStatus() == DownloadStatus.PROCESSING) {
                    file.setStatus(DownloadStatus.PAUSED);
                    binding.status.setText(file.getStatus().getValue());
                }
            }

            public void destroy() {
                if (task == null)
                    return;
                LogHelper.d(TAG, "Download item recycled");
                task.getProgress().removeObserver(progressChangeObserver);
                task.getStatus().removeObserver(statusChangeObserver);
            }

            private final Observer<DownloadStatus> statusChangeObserver = status -> {
                file.setStatus(status);
                if (status == DownloadStatus.DOWNLOADED) {
                    Download download = DownloadRepository.getInstance(context).getDownloadDao().find(file.getId());
                    file.setFileLength(download.getFileLength());
                    file.setUri(download.getUri());
                    binding.status.setText("");
                    binding.fileSize.setText(ConverterUtil.getFormattedFileSize(download.getFileLength()));
                    return;
                }
                binding.status.setText(status.getValue());
            };

            private final Observer<DownloadTask.DownloadProgress> progressChangeObserver = progress -> {
                binding.fileSize.setText(String.format(Locale.US, "%s / %s", ConverterUtil.getFormattedFileSize(progress.receivedBytes), ConverterUtil.getFormattedFileSize(progress.totalBytes)));
            };

        }

        public interface OnClickListener {
            void onClick(Download file, int adapterPosition);
        }

    }

    private final ActivityResultLauncher<IntentSenderRequest> launcher = registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show();
                }
            });
}