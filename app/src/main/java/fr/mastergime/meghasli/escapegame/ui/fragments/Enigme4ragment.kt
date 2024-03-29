package fr.mastergime.meghasli.escapegame.ui.fragments

import android.media.MediaPlayer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import fr.mastergime.meghasli.escapegame.R
import fr.mastergime.meghasli.escapegame.databinding.FragmentEnigme4ragmentBinding

class Enigme4ragment : Fragment(R.layout.fragment_enigme4ragment) {


    private lateinit var binding: FragmentEnigme4ragmentBinding
    private lateinit var  mediaPlayer: MediaPlayer

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.audio_enigme_final)

        binding = FragmentEnigme4ragmentBinding.bind(view)

    }

}