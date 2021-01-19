package com.example.daa.data.model

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.text.TextRecognizer

class MainModel(app : Application) {
    private val application = app
    private lateinit var audioPlayer: CustomAudioPlayer
    private lateinit var textRecognizer: TextRecognizer
    private lateinit var tts : TextToSpeech
    init {

    }
}