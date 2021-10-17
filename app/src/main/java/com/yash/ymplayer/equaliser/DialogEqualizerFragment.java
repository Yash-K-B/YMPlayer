package com.yash.ymplayer.equaliser;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.PresetReverb;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.google.gson.Gson;
import com.yash.ymplayer.R;
import com.yash.logging.LogHelper;
import com.yash.ymplayer.util.EqualizerUtil;

import java.util.ArrayList;


public class DialogEqualizerFragment extends DialogFragment {
    public static final String ARG_AUDIO_SESSIOIN_ID = "audio_session_id";
    private static final String TAG = DialogEqualizerFragment.class.getSimpleName();
    private static int accentAlpha = Color.BLUE;
    private static int darkBackground = Color.GRAY;
    private static int textColor = Color.WHITE;
    private static int themeColor = Color.parseColor("#B24242");
    private static int backgroundColor = Color.BLACK;
    private static int themeRes = 0;
    private static String titleString = "";
    private static int titleRes = 0;

    private final int TARGET_GAIN_MAX = 1000; //10dB = 1000mdB
    private Equalizer mEqualizer;
    private BassBoost bassBoost;
    private PresetReverb presetReverb;
    private LoudnessEnhancer loudnessEnhancer;
    private LineSet dataset;
    private LineChartView chart;
    private float[] points;
    private int y = 0;
    private int z = 0;
    private final SeekBar[] seekBarFinal = new SeekBar[5];
    private Spinner presetSpinner;
    private Context ctx;
    private int audioSesionId;
    private TextView titleTextView;
    private AnalogController bassController;
    private AnalogController reverbController;
    private AnalogController loudnessController;


    public DialogEqualizerFragment() {
        // Required empty public constructor
    }

    private static DialogEqualizerFragment newInstance(int audioSessionId) {

        Bundle args = new Bundle();
        args.putInt(ARG_AUDIO_SESSIOIN_ID, audioSessionId);

        DialogEqualizerFragment fragment = new DialogEqualizerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public int getTheme() {
        if (themeRes != 0) return themeRes;
        else return super.getTheme();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings.isEditing = true;

        mEqualizer = EqualizerUtil.getInstance(getContext()).getEqualizer().getValue();
        bassBoost = EqualizerUtil.getInstance(getContext()).getBassBoost().getValue();
        presetReverb = EqualizerUtil.getInstance(getContext()).getPresetReverb().getValue();
        loudnessEnhancer = EqualizerUtil.getInstance(getContext()).getLoudnessEnhancer().getValue();

        EqualizerUtil.getInstance(getContext()).getEqualizer().observe(requireActivity(), equalizer -> mEqualizer = equalizer);
        EqualizerUtil.getInstance(getContext()).getBassBoost().observe(requireActivity(), bassBoost -> this.bassBoost = bassBoost);
        EqualizerUtil.getInstance(getContext()).getPresetReverb().observe(requireActivity(), presetReverb -> this.presetReverb = presetReverb);
        EqualizerUtil.getInstance(getContext()).getLoudnessEnhancer().observe(requireActivity(), loudnessEnhancer -> this.loudnessEnhancer = loudnessEnhancer);

    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ctx = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_fragment_equalizer, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView backBtn = view.findViewById(R.id.equalizer_back_btn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        backBtn.setColorFilter(textColor);
        view.findViewById(R.id.equalizerLayout).setBackgroundColor(backgroundColor);

        titleTextView = view.findViewById(R.id.equalizer_fragment_title);
        titleTextView.setTextColor(textColor);
        if (titleRes != 0) {
            try {
                titleTextView.setText(getString(titleRes));
            } catch (Exception e) {
                Log.e(TAG, "onViewCreated: unable to set title because " + e.getLocalizedMessage());
            }
        } else if (!TextUtils.isEmpty(titleString)) {
            titleTextView.setText(titleString);
        }
        SwitchCompat equalizerSwitch = view.findViewById(R.id.equalizer_switch);
        equalizerSwitch.setChecked(Settings.isEqualizerEnabled);
        equalizerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                EqualizerUtil.getInstance(getContext()).setEqualizerEnabled(isChecked);
                LogHelper.d(TAG, "onCheckedChanged: ");
            }
        });


        presetSpinner = view.findViewById(R.id.equalizer_preset_spinner);
        presetSpinner.getBackground().setColorFilter(new PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_ATOP));

        chart = view.findViewById(R.id.lineChart);
        Paint paint = new Paint();
        dataset = new LineSet();

        bassController = view.findViewById(R.id.controllerBass);
        reverbController = view.findViewById(R.id.controller3D);
        loudnessController = view.findViewById(R.id.controllerLoudness);

        bassController.setLabel("BASS");
        reverbController.setLabel("3D");
        loudnessController.setLabel("LOUDNESS");


        bassController.circlePaint2.setColor(themeColor);
        bassController.linePaint.setColor(themeColor);
        bassController.invalidate();
        reverbController.circlePaint2.setColor(themeColor);
        reverbController.linePaint.setColor(themeColor);
        reverbController.invalidate();
        loudnessController.circlePaint2.setColor(themeColor);
        loudnessController.linePaint.setColor(themeColor);
        loudnessController.invalidate();

        if (!Settings.isEqualizerReloaded) {
            int x = 0;
            if (bassBoost != null) {
                try {
                    x = ((bassBoost.getRoundedStrength() * 19) / 1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (presetReverb != null) {
                try {
                    y = (presetReverb.getPreset() * 19) / 6;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (loudnessEnhancer != null) {
                try {
                    z = (int) ((Settings.loudnessGain * 18) / Settings.TargetLoudnessGain) + 1;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (x == 0) {
                bassController.setProgress(1);
            } else {
                bassController.setProgress(x);
            }

            if (y == 0) {
                reverbController.setProgress(1);
            } else {
                reverbController.setProgress(y);
            }

            if (z == 0) {
                loudnessController.setProgress(1);
            } else {
                loudnessController.setProgress(z);
            }
        } else {
            int x = ((Settings.bassStrength * 19) / 1000);
            y = (Settings.reverbPreset * 19) / 6;
            z = Math.round((Settings.loudnessGain * 18.0f) / Settings.TargetLoudnessGain) + 1;
            if (x == 0) {
                bassController.setProgress(1);
            } else {
                bassController.setProgress(x);
            }

            if (y == 0) {
                reverbController.setProgress(1);
            } else {
                reverbController.setProgress(y);
            }

            if (z == 0) {
                loudnessController.setProgress(1);
            } else {
                loudnessController.setProgress(z);
            }
        }

        bassController.setOnProgressChangedListener(new AnalogController.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                Settings.bassStrength = (short) (((float) 1000 / 19) * (progress));
                try {
                    bassBoost.setStrength(Settings.bassStrength);
                    Settings.equalizerModel.setBassStrength(Settings.bassStrength);
                    EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        reverbController.setOnProgressChangedListener(new AnalogController.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                Settings.reverbPreset = (short) ((progress * 6) / 19);
                Settings.equalizerModel.setReverbPreset(Settings.reverbPreset);
                try {
                    presetReverb.setPreset(Settings.reverbPreset);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                y = progress;
                EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
            }
        });

        loudnessController.setOnProgressChangedListener(new AnalogController.onProgressChangedListener() {
            @Override
            public void onProgressChanged(int progress) {
                Settings.loudnessGain = ((progress - 1) * Settings.TargetLoudnessGain) / 18;
                Settings.equalizerModel.setLoudnessGain(Settings.loudnessGain);
                try {
                    loudnessEnhancer.setTargetGain(Settings.loudnessGain);
                    EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                z = progress;
            }
        });

        TextView equalizerHeading = new TextView(ctx);
        equalizerHeading.setText(R.string.eq);
        equalizerHeading.setTextSize(20);
        equalizerHeading.setGravity(Gravity.CENTER_HORIZONTAL);

        short numberOfFrequencyBands = 5;

        points = new float[numberOfFrequencyBands];

        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];
        final short upperEqualizerBandLevel = mEqualizer.getBandLevelRange()[1];

        for (short i = 0; i < numberOfFrequencyBands; i++) {
            final short equalizerBandIndex = i;
            final TextView frequencyHeaderTextView = new TextView(ctx);
            frequencyHeaderTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            frequencyHeaderTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            frequencyHeaderTextView.setTextColor(textColor);
            frequencyHeaderTextView.setText((mEqualizer.getCenterFreq(equalizerBandIndex) / 1000) + "Hz");

            LinearLayout seekBarRowLayout = new LinearLayout(ctx);
            seekBarRowLayout.setOrientation(LinearLayout.VERTICAL);

            TextView lowerEqualizerBandLevelTextView = new TextView(ctx);
            lowerEqualizerBandLevelTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            lowerEqualizerBandLevelTextView.setTextColor(textColor);
            lowerEqualizerBandLevelTextView.setText((lowerEqualizerBandLevel / 100) + "dB");

            TextView upperEqualizerBandLevelTextView = new TextView(ctx);
            lowerEqualizerBandLevelTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            upperEqualizerBandLevelTextView.setTextColor(textColor);
            upperEqualizerBandLevelTextView.setText((upperEqualizerBandLevel / 100) + "dB");

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.weight = 1;

            SeekBar seekBar = new SeekBar(ctx);
            TextView textView = new TextView(ctx);
            switch (i) {
                case 0:
                    seekBar = view.findViewById(R.id.seekBar1);
                    textView = view.findViewById(R.id.textView1);
                    break;
                case 1:
                    seekBar = view.findViewById(R.id.seekBar2);
                    textView = view.findViewById(R.id.textView2);
                    break;
                case 2:
                    seekBar = view.findViewById(R.id.seekBar3);
                    textView = view.findViewById(R.id.textView3);
                    break;
                case 3:
                    seekBar = view.findViewById(R.id.seekBar4);
                    textView = view.findViewById(R.id.textView4);
                    break;
                case 4:
                    seekBar = view.findViewById(R.id.seekBar5);
                    textView = view.findViewById(R.id.textView5);
                    break;
            }
            seekBarFinal[i] = seekBar;
            seekBar.getProgressDrawable().setColorFilter(new PorterDuffColorFilter(Color.DKGRAY, PorterDuff.Mode.SRC_IN));
            seekBar.getThumb().setColorFilter(new PorterDuffColorFilter(themeColor, PorterDuff.Mode.SRC_IN));
            seekBar.setId(i);
//            seekBar.setLayoutParams(layoutParams);
            seekBar.setMax(upperEqualizerBandLevel - lowerEqualizerBandLevel);

            textView.setText(frequencyHeaderTextView.getText());
            textView.setTextColor(textColor);
            textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            if (Settings.isEqualizerReloaded) {
                points[i] = Settings.seekbarpos[i] - lowerEqualizerBandLevel;
                dataset.addPoint(frequencyHeaderTextView.getText().toString(), points[i]);
                seekBar.setProgress(Settings.seekbarpos[i] - lowerEqualizerBandLevel);
            } else {
                points[i] = mEqualizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel;
                dataset.addPoint(frequencyHeaderTextView.getText().toString(), points[i]);
                seekBar.setProgress(mEqualizer.getBandLevel(equalizerBandIndex) - lowerEqualizerBandLevel);
                Settings.seekbarpos[i] = mEqualizer.getBandLevel(equalizerBandIndex);
                Settings.isEqualizerReloaded = true;
            }
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // LogHelper.d(TAG, "onProgressChanged: fromUser :"+fromUser);
                    if (fromUser) {
                        mEqualizer.setBandLevel(equalizerBandIndex, (short) (progress + lowerEqualizerBandLevel));
                        Settings.seekbarpos[seekBar.getId()] = (progress + lowerEqualizerBandLevel);
                        Settings.equalizerModel.getSeekbarpos()[seekBar.getId()] = (progress + lowerEqualizerBandLevel);
                    }
                    points[seekBar.getId()] = (progress);
                    dataset.updateValues(points);
                    chart.notifyDataUpdate();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    presetSpinner.setSelection(0);
                    Settings.presetPos = 0;
                    Settings.equalizerModel.setPresetPos(0);
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    LogHelper.d(TAG, "onStopTrackingTouch: ");
                    EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
                }
            });

        }

        equalizeSound();

        paint.setColor(textColor);
        paint.setStrokeWidth((float) (1.10 * Settings.ratio));

        dataset.setColor(themeColor);
        dataset.setSmooth(true);
        dataset.setThickness(5);

        chart.setXAxis(false);
        chart.setYAxis(false);

        chart.setYLabels(AxisController.LabelPosition.NONE);
        chart.setXLabels(AxisController.LabelPosition.NONE);
        chart.setGrid(ChartView.GridType.NONE, 7, 10, paint);

        chart.setAxisBorderValues(-300, 3300);

        chart.addData(dataset);
        chart.show();

        Button mEndButton = new Button(ctx);
        mEndButton.setBackgroundColor(themeColor);
        mEndButton.setTextColor(textColor);


    }


    public TextView getTitleTextView() {
        return titleTextView;
    }

    public AnalogController getBassController() {
        return bassController;
    }

    public AnalogController getReverbController() {
        return reverbController;
    }

    public AnalogController getLoudnessController() {
        return loudnessController;
    }

    public void equalizeSound() {
        ArrayList<String> equalizerPresetNames = new ArrayList<>();
        ArrayAdapter<String> equalizerPresetSpinnerAdapter = new ArrayAdapter<>(ctx,
                R.layout.spinner_item,
                equalizerPresetNames);
        equalizerPresetSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        equalizerPresetNames.add("Custom");

        for (short i = 0; i < mEqualizer.getNumberOfPresets(); i++) {
            equalizerPresetNames.add(mEqualizer.getPresetName(i));
        }

        presetSpinner.setAdapter(equalizerPresetSpinnerAdapter);
        if (Settings.isEqualizerReloaded && Settings.presetPos != 0) {
            presetSpinner.setSelection(Settings.presetPos);
        }

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (position != 0) {
                        mEqualizer.usePreset((short) (position - 1));
                        Settings.presetPos = position;
                        short numberOfFreqBands = 5;

                        final short lowerEqualizerBandLevel = mEqualizer.getBandLevelRange()[0];

                        for (short i = 0; i < numberOfFreqBands; i++) {
                            ObjectAnimator.ofInt(seekBarFinal[i], "progress", (mEqualizer.getBandLevel(i) - lowerEqualizerBandLevel)).setDuration(400).start();
                            //seekBarFinal[i].setProgress(mEqualizer.getBandLevel(i) - lowerEqualizerBandLevel);
                            //points[i]                                  = mEqualizer.getBandLevel(i) - lowerEqualizerBandLevel;
                            Settings.seekbarpos[i] = mEqualizer.getBandLevel(i);
                            Settings.equalizerModel.getSeekbarpos()[i] = mEqualizer.getBandLevel(i);
                        }
//                        dataset.updateValues(points);
//                        chart.notifyDataUpdate();
                    }
                } catch (Exception e) {
                    Toast.makeText(ctx, "Error while updating Equalizer", Toast.LENGTH_SHORT).show();
                }
                Settings.equalizerModel.setPresetPos(position);
                EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EqualizerUtil.getInstance(getContext()).saveSettingsToDevice();
        Settings.isEditing = false;

    }

    public static class Builder {
        private int id = -1;

        public Builder setThemeRes(int res) {
            themeRes = res;
            return this;
        }

        public Builder setAudioSessionId(int id) {
            this.id = id;
            return this;
        }

        public Builder setAccentColor(int color) {
            themeColor = color;
            return this;
        }

        public Builder themeColor(int color) {
            backgroundColor = color;
            return this;
        }

        public Builder textColor(int color) {
            textColor = color;
            return this;
        }

        public Builder darkColor(int color) {
            darkBackground = color;
            return this;
        }

        public Builder accentAlpha(int color) {
            accentAlpha = color;
            return this;
        }

        public Builder title(@StringRes int title) {
            titleRes = title;
            return this;
        }

        public Builder title(@NonNull String title) {
            titleString = title;
            return this;
        }

        public DialogEqualizerFragment build() {
            return DialogEqualizerFragment.newInstance(id);
        }
    }

}