package com.example.daa.data.model

import android.app.Application
import android.content.Context
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import com.example.daa.ui.view.UTTERANCE_ID
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.lang.Error
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume


class CustomTTS {
    //SET UTTERANCE PROGRESS LISTENER
    lateinit var audioFile: File
    private lateinit var tts : TextToSpeech
    fun initTTS(application: Application, listener : TextToSpeech.OnInitListener) {
        tts = TextToSpeech(application, listener)
    }
    suspend fun textToAudioFile(text : String, file : File) = suspendCancellableCoroutine<File> {continuation ->
        val listener = object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                continuation.resume(file)
            }
            override fun onError(utteranceId: String?) {
                throw Error("utterance progress listener error")
            }
            override fun onStart(utteranceId: String?) {
//                TODO("Not yet implemented")
            }
        }
        tts.setOnUtteranceProgressListener(listener)
        synthesizeToAudioFile(text, file)
    }
    private fun synthesizeToAudioFile(text : String, file : File){
        if(!this::tts.isInitialized){throw Error("Cant convert text before tts engine is initialized")}
        tts.synthesizeToFile(prepareTextInput(text), null, file, UTTERANCE_ID)
    }
    private fun prepareTextInput(text : String) : String{
        var newText = text.replace("\n", " ")
        val max = TextToSpeech.getMaxSpeechInputLength()
        if(newText.length > max){
            newText = newText.take(max)
        }
        return newText
    }
    fun createAudioFile(context: Context) : File {
        // Create an image file
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File.createTempFile(
            "WAV_${timeStamp}_", /* prefix */
            ".wav", /* suffix */
            storageDir /* directory */
        )
    }
}