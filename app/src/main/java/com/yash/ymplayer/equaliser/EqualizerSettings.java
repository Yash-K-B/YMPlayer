package com.yash.ymplayer.equaliser;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class EqualizerSettings {
    public int[] seekbarpos = new int[5];
    public int presetPos;
    public short reverbPreset;
    public short bassStrength;
    public int loudnessGain;
    public int targetLoudnessGain;
    public boolean isEqualizerEnabled;

    @Override
    public String toString() {
        return "EqualizerSettings{" +
                "seekbarpos=" + Arrays.toString(seekbarpos) +
                ", presetPos=" + presetPos +
                ", reverbPreset=" + reverbPreset +
                ", bassStrength=" + bassStrength +
                ", loudnessGain=" + loudnessGain +
                ", targetLoudnessGain=" + targetLoudnessGain +
                ", isEqualizerEnabled=" + isEqualizerEnabled +
                '}';
    }
}