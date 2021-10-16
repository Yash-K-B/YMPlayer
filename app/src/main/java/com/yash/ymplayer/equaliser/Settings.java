package com.yash.ymplayer.equaliser;

public class Settings {
    public static boolean isEqualizerEnabled = true;
    public static boolean isEqualizerReloaded = true;
    public static int[] seekbarpos = new int[5];
    public static int presetPos;
    public static short reverbPreset = -1, bassStrength = -1;
    public static int loudnessGain = 0;
    public static EqualizerModel equalizerModel;
    public static double ratio = 1.0;
    public static boolean isEditing = false;
    public static int TargetLoudnessGain = 1000;
}