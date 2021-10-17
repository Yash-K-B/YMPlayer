package com.yash.ymplayer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.PresetReverb;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.equaliser.EqualizerModel;
import com.yash.ymplayer.equaliser.EqualizerSettings;
import com.yash.ymplayer.equaliser.Settings;

import java.util.Objects;
import java.util.Observable;

public class EqualizerUtil {
    private static final String TAG = "EqualizerUtil";

    public static final String PREF_KEY = "equalizer";

    private boolean equalizerEnabled;
    SharedPreferences appPreferences;

    private final MutableLiveData<Equalizer> equalizer;
    private final MutableLiveData<BassBoost> bassBoost;
    private final MutableLiveData<PresetReverb> presetReverb;
    private final MutableLiveData<LoudnessEnhancer> loudnessEnhancer;


    private static EqualizerUtil instance;

    public static EqualizerUtil getInstance(Context context) {
        if (instance == null)
            instance = new EqualizerUtil(context.getApplicationContext());
        return instance;
    }

    private EqualizerUtil(Context context) {
        equalizerEnabled = false;
        appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        equalizer = new MutableLiveData<>(new Equalizer(0,0));
        bassBoost = new MutableLiveData<>(new BassBoost(0,0 ));
        presetReverb = new MutableLiveData<>(new PresetReverb(0,0));
        loudnessEnhancer = new MutableLiveData<>(new LoudnessEnhancer(0));
        loadSettingsFromDevice();
        initEqualizer();
    }


    public void updateAudioSessionId(int audioSessionId) {
        reloadEqualizer(audioSessionId);
    }


    public void release() {
        if (equalizer.getValue() != null) {
            equalizer.getValue().release();
        }
        if (bassBoost.getValue() != null) {
            bassBoost.getValue().release();
        }
        if (presetReverb.getValue() != null) {
            presetReverb.getValue().release();
        }
        if (loudnessEnhancer.getValue() != null) {
            loudnessEnhancer.getValue().release();
        }
    }

    private void initEqualizer() {

        //BassBoost
        Objects.requireNonNull(bassBoost.getValue()).setEnabled(Settings.isEqualizerEnabled);
        BassBoost.Settings bassBoostSettingTemp = bassBoost.getValue().getProperties();
        BassBoost.Settings bassBoostSetting = new BassBoost.Settings(bassBoostSettingTemp.toString());
        bassBoostSetting.strength = Settings.equalizerModel.getBassStrength();
        bassBoost.getValue().setProperties(bassBoostSetting);

        //PresetReverb
        Objects.requireNonNull(presetReverb.getValue()).setPreset(Settings.equalizerModel.getReverbPreset());
        presetReverb.getValue().setEnabled(Settings.isEqualizerEnabled);

        //LoudnessEnhancer
        Objects.requireNonNull(loudnessEnhancer.getValue()).setTargetGain(Settings.loudnessGain);
        loudnessEnhancer.getValue().setEnabled(Settings.isEqualizerEnabled);

        //Equalizer
        if (Settings.presetPos == 0) {
            for (short bandIdx = 0; bandIdx < Objects.requireNonNull(equalizer.getValue()).getNumberOfBands(); bandIdx++) {
                equalizer.getValue().setBandLevel(bandIdx, (short) Settings.seekbarpos[bandIdx]);
            }
        } else {
            Objects.requireNonNull(equalizer.getValue()).usePreset((short) (Settings.presetPos - 1));
        }
        equalizer.getValue().setEnabled(Settings.isEqualizerEnabled);
    }

    private void reloadEqualizer(int audioSessionId) {
        release();

        //BassBoost
        bassBoost.setValue(new BassBoost(0, audioSessionId));
        Objects.requireNonNull(bassBoost.getValue()).setEnabled(Settings.isEqualizerEnabled);
        BassBoost.Settings bassBoostSettingTemp = bassBoost.getValue().getProperties();
        BassBoost.Settings bassBoostSetting = new BassBoost.Settings(bassBoostSettingTemp.toString());
        bassBoostSetting.strength = Settings.equalizerModel.getBassStrength();
        bassBoost.getValue().setProperties(bassBoostSetting);

        //PresetReverb
        presetReverb.setValue(new PresetReverb(0, audioSessionId));
        Objects.requireNonNull(presetReverb.getValue()).setPreset(Settings.equalizerModel.getReverbPreset());
        presetReverb.getValue().setEnabled(Settings.isEqualizerEnabled);

        //LoudnessEnhancer
        loudnessEnhancer.setValue(new LoudnessEnhancer(audioSessionId));
        Objects.requireNonNull(loudnessEnhancer.getValue()).setTargetGain(Settings.loudnessGain);
        loudnessEnhancer.getValue().setEnabled(Settings.isEqualizerEnabled);

        //Equalizer
        equalizer.setValue(new Equalizer(0, audioSessionId));
        if (Settings.presetPos == 0) {
            for (short bandIdx = 0; bandIdx < Objects.requireNonNull(equalizer.getValue()).getNumberOfBands(); bandIdx++) {
                equalizer.getValue().setBandLevel(bandIdx, (short) Settings.seekbarpos[bandIdx]);
            }
        } else {
            Objects.requireNonNull(equalizer.getValue()).usePreset((short) (Settings.presetPos - 1));
        }
        equalizer.getValue().setEnabled(Settings.isEqualizerEnabled);
    }


    public void loadSettingsFromDevice() {
        Gson gson = new Gson();
        EqualizerSettings settings = gson.fromJson(appPreferences.getString(PREF_KEY, "{}"), EqualizerSettings.class);

        Log.d(TAG, "loadSettingsFromDevice: " + settings);

        EqualizerModel model = new EqualizerModel();
        model.setBassStrength(settings.bassStrength);
        model.setPresetPos(settings.presetPos);
        model.setReverbPreset(settings.reverbPreset);
        model.setSeekbarpos(settings.seekbarpos);
        model.setLoudnessGain(settings.loudnessGain);

        Settings.isEqualizerEnabled = settings.isEqualizerEnabled;
        Settings.isEqualizerReloaded = true;
        Settings.bassStrength = settings.bassStrength;
        Settings.presetPos = settings.presetPos;
        Settings.reverbPreset = settings.reverbPreset;
        Settings.seekbarpos = settings.seekbarpos;
        Settings.loudnessGain = settings.loudnessGain;
        Settings.TargetLoudnessGain  = Integer.parseInt(appPreferences.getString(Keys.PREFERENCE_KEYS.LOUDNESS_GAIN, "1000"));
        Settings.equalizerModel = model;
    }

    public void saveSettingsToDevice() {
        if (Settings.equalizerModel != null) {
            EqualizerSettings settings = new EqualizerSettings();
            settings.bassStrength = Settings.equalizerModel.getBassStrength();
            settings.presetPos = Settings.equalizerModel.getPresetPos();
            settings.reverbPreset = Settings.equalizerModel.getReverbPreset();
            settings.loudnessGain = Settings.loudnessGain;
            settings.seekbarpos = Settings.equalizerModel.getSeekbarpos();
            settings.isEqualizerEnabled = Settings.isEqualizerEnabled;
            settings.targetLoudnessGain = Settings.TargetLoudnessGain;

            Gson gson = new Gson();
            appPreferences.edit()
                    .putString(PREF_KEY, gson.toJson(settings))
                    .apply();
        }
    }

    public boolean isEqualizerEnabled() {
        return equalizerEnabled;
    }

    public void setEqualizerEnabled(boolean equalizerEnabled) {
        Settings.isEqualizerEnabled = equalizerEnabled;
        this.equalizerEnabled = equalizerEnabled;
        Settings.equalizerModel.setEqualizerEnabled(equalizerEnabled);
        saveSettingsToDevice();
        enableEqualizer(equalizerEnabled);
    }

    public void setEqualizerEnabled(boolean equalizerEnabled, int audioSessionId) {
        Settings.isEqualizerEnabled = equalizerEnabled;
        this.equalizerEnabled = equalizerEnabled;
        Settings.equalizerModel.setEqualizerEnabled(equalizerEnabled);
        saveSettingsToDevice();
        reloadEqualizer(audioSessionId);
    }

    private void enableEqualizer(boolean isEnabled) {
        if (equalizer.getValue() != null)
            equalizer.getValue().setEnabled(isEnabled);
        if (bassBoost.getValue() != null)
            bassBoost.getValue().setEnabled(isEnabled);
        if (presetReverb.getValue() != null)
            presetReverb.getValue().setEnabled(isEnabled);
        if (loudnessEnhancer.getValue() != null)
            loudnessEnhancer.getValue().setEnabled(isEnabled);
        Settings.isEqualizerEnabled = isEnabled;
        Settings.equalizerModel.setEqualizerEnabled(isEnabled);
        LogHelper.d(TAG, "onCheckedChanged: ");
        saveSettingsToDevice();
    }

    public void reloadLoudnessGain(int gain) {
        double fraction = (double) Settings.loudnessGain / Settings.TargetLoudnessGain;
        Settings.loudnessGain = (int) (fraction * gain);
        if (loudnessEnhancer.getValue() != null) {
            loudnessEnhancer.getValue().setTargetGain(Settings.loudnessGain);
            Log.d(TAG, "reloadLoudnessGain: "+ loudnessEnhancer.getValue().getTargetGain());
        }
        Settings.TargetLoudnessGain = gain;
    }

    public MutableLiveData<Equalizer> getEqualizer() {
        return equalizer;
    }

    public MutableLiveData<BassBoost> getBassBoost() {
        return bassBoost;
    }

    public MutableLiveData<PresetReverb> getPresetReverb() {
        return presetReverb;
    }

    public MutableLiveData<LoudnessEnhancer> getLoudnessEnhancer() {
        return loudnessEnhancer;
    }
}
