@file:JvmName("AboutFragment")
package com.yash.ymplayer.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import com.yash.ymplayer.R
import com.yash.ymplayer.databinding.FragmentAboutBinding
import com.yash.ymplayer.interfaces.ActivityActionProvider

class AboutFragment : Fragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding = FragmentAboutBinding.inflate(inflater, container, false)
        binding.authorImage.clipToOutline = true
        binding.licenses.setOnClickListener { v: View? ->
            MaterialDialog(requireContext()).show {
                title(text = "Licenses")
                message(text = """
                        YoutubeExtractor - Extraction of youtube video links.

                        AndroidEqualizer - Audio Equalizer for player.

                        AndroidLame - Wrapper library for Android / Java around Lame MP3 encoder.

                        mp3agic - A java library for reading mp3 files and reading / manipulating the ID3 tags (ID3v1 and ID3v2.2 through ID3v2.4)

                        Glide - Open Source Media Management and Image Loading Framework for Android
                    """.trimIndent())
            }
        }

        binding.changeLog.setOnClickListener { v: View? ->
            MaterialDialog(requireContext()).show {
                title(text = "Change Logs")
                message(text = resources.getString(R.string.changelog).trimIndent())
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (context as ActivityActionProvider?)!!.setCustomToolbar(null, "About")
    }
}