package com.yash.ymplayer.ffmpeg;

import lombok.Builder;
import lombok.Getter;

public class AudioFile {
    private final String inputFile;
    private final String outputFile;
    private final AudioMetadata audioMetadata;
    private final String bitrate;
    private final String sampleRate;
    private final Integer channelCount;

    private AudioFile(Builder builder) {
        inputFile = builder.inputFile;
        outputFile = builder.outputFile;
        audioMetadata = builder.audioMetadata;
        bitrate = builder.bitrate;
        sampleRate = builder.sampleRate;
        channelCount = builder.channelCount;
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public AudioMetadata getAudioMetadata() {
        return audioMetadata;
    }

    public String getBitrate() {
        return bitrate;
    }

    public String getSampleRate() {
        return sampleRate;
    }

    public Integer getChannelCount() {
        return channelCount;
    }

    public static final class Builder {
        private String inputFile;
        private String outputFile;
        private AudioMetadata audioMetadata;
        private String bitrate;
        private String sampleRate;
        private Integer channelCount;

        public Builder() {
        }

        public Builder inputFile(String val) {
            inputFile = val;
            return this;
        }

        public Builder outputFile(String val) {
            outputFile = val;
            return this;
        }

        public Builder audioMetadata(AudioMetadata val) {
            audioMetadata = val;
            return this;
        }

        public Builder bitrate(String val) {
            bitrate = val;
            return this;
        }

        public Builder sampleRate(String val) {
            sampleRate = val;
            return this;
        }

        public Builder channelCount(int channelCount) {
            this.channelCount = channelCount;
            return this;
        }

        public AudioFile build() {
            return new AudioFile(this);
        }
    }
}