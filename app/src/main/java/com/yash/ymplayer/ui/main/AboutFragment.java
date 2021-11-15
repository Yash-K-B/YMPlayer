package com.yash.ymplayer.ui.main;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.yash.ymplayer.ActivityActionProvider;
import com.yash.ymplayer.R;
import com.yash.ymplayer.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    Context context;

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        FragmentAboutBinding binding = FragmentAboutBinding.inflate(inflater,container,false);
        binding.authorImage.setClipToOutline(true);
        binding.licenses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ActivityActionProvider)context).setCustomToolbar(null,"About");
    }
}