package com.yash.ymplayer.equaliser;

import androidx.annotation.NonNull;

public class EqualizerSettings {
    public int[] seekbarpos = new int[5];
    public int presetPos;
    public short reverbPreset;
    public short bassStrength;
    public int loudnessGain;
    public boolean isEqualizerEnabled;

    @NonNull
    public String toString() {
        return "Preset Position : " + presetPos + "\n" +
                "Reverb Preset : " + reverbPreset + "\n" +
                "Bass Strength : " + bassStrength + "\n" +
                "Loudness Gain : " + loudnessGain + "\n" +
                "Seek bar pos : " + seekbarpos[0] + "-" + seekbarpos[1] + "-" + seekbarpos[2] + "-" + seekbarpos[3] + "-" + seekbarpos[4] + "\n" +
                "Is Equalizer Enabled : " + isEqualizerEnabled;
    }
}