package com.yash.ymplayer.ffmpeg;

import android.content.Context;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.StatisticsCallback;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class FFMpegUtil {
    private static final String TAG = "FFMpegUtil";

    public static void convert(AudioFile audioFile, OnConversionListener listener) {
        List<String> commands = new ArrayList<>();

        commands.add("-y");

        commands.add("-i");
        commands.add(String.format("\"%s\"", audioFile.getInputFile()));

        if(audioFile.getAudioMetadata() != null && StringUtil.hasText(audioFile.getAudioMetadata().getAlbumArt())) {
            commands.add(String.format("-i \"%s\"", audioFile.getAudioMetadata().getAlbumArt()));
        }

        if(StringUtil.hasText(audioFile.getSampleRate())) {
            commands.add("-ar");
            commands.add(audioFile.getSampleRate());
        }

        if(StringUtil.hasText(audioFile.getBitrate())) {
            commands.add("-b:a");
            commands.add(audioFile.getBitrate());
        }

        if(StringUtil.hasText(audioFile.getBitrate())) {
            commands.add("-b:a");
            commands.add(audioFile.getBitrate());
        }

        if(audioFile.getChannelCount() != null) {
            commands.add("-ac");
            commands.add(String.valueOf(audioFile.getChannelCount()));
        }

        addCodec(audioFile, commands);

        addCommandsForMetadata(audioFile, commands);

        commands.add(String.format("\"%s\"", audioFile.getOutputFile()));

        String query = String.join( " ", commands);
        LogHelper.d(TAG, "Query to be executed in ffmpeg: %s", query);

        FFmpegSession session = FFmpegKit.execute(query);
        LogHelper.d(TAG, "convert: " + session);
        if (ReturnCode.isSuccess(session.getReturnCode())) {
           listener.onSuccess(audioFile);
        } else if (ReturnCode.isCancel(session.getReturnCode())) {
            listener.onFailed(audioFile);
        } else {
            listener.onFailed(audioFile);
            LogHelper.d(TAG, String.format("Command failed with state %s and rc %s.%s", session.getState(), session.getReturnCode(), session.getFailStackTrace()));
        }

    }

    private static void addCodec(AudioFile audioFile, List<String> commands) {
       if (audioFile.getOutputFile().endsWith(".aac"))
            commands.add("-c:a aac");
    }

    private static void addCommandsForMetadata(AudioFile audioFile, List<String> commands) {
        if (audioFile.getAudioMetadata() != null) {
            AudioMetadata audioMetadata = audioFile.getAudioMetadata();
            if (StringUtil.hasText(audioMetadata.getAlbumArt())) {
                commands.add("-id3v2_version 3");
                commands.add("-map_metadata 0");
                commands.add("-metadata:s:t mimetype=image/png");
                commands.add("-map 0:a -map 1:v");
            } else {
                commands.add("-vn ");
            }
            if (StringUtil.hasText(audioMetadata.getTitle())) {
                commands.add(String.format("-metadata title=\"%s\"", audioMetadata.getTitle()));
            }
            if (StringUtil.hasText(audioMetadata.getAlbum())) {
                commands.add(String.format("-metadata album=\"%s\"", audioMetadata.getAlbum()));
            }
            if (StringUtil.hasText(audioMetadata.getArtist())) {
                commands.add(String.format("-metadata artist=\"%s\"", audioMetadata.getArtist()));
            }
            if (StringUtil.hasText(audioMetadata.getComposer())) {
                commands.add(String.format("-metadata composer=\"%s\"", audioMetadata.getComposer()));
            }
            if (StringUtil.hasText(audioMetadata.getGenre())) {
                commands.add(String.format("-metadata genre=\"%s\"", audioMetadata.getGenre()));
            }
            if (StringUtil.hasText(audioMetadata.getYear())) {
                commands.add(String.format("-metadata year=\"%s\"", audioMetadata.getYear()));
            }
        } else {
            commands.add("-vn ");
        }
    }


    public interface OnConversionListener {
        void onSuccess(AudioFile audioFile);

        void onFailed(AudioFile audioFile);
    }
}
