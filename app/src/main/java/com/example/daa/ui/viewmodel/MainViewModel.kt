package com.example.daa.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.daa.data.model.CustomAudioPlayer
import com.example.daa.data.model.CustomOCR
import com.example.daa.data.model.CustomTTS
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.resume

class MainViewModel : ViewModel() {

    private lateinit var application: Application
    private var viewModelInitialied = false

    //Seekbar updater thread
    val updateThread = newSingleThreadContext("Update Seek Bar")

    //UI STATE VARIABLES

    //LiveData
    var ttsInitialized : MutableLiveData<Boolean> = MutableLiveData<Boolean>().also{it.value=false}
    var takenPhoto : MutableLiveData<Bitmap> = MutableLiveData<Bitmap>()
    var displayPlayerControls : MutableLiveData<Boolean> = MutableLiveData<Boolean>().also{it.value=false}

        //OCR VARIABLES
    private lateinit var imageFile : File
    private lateinit var imageUri : Uri

        //CAP VARIABLES
    private lateinit var audioFile : File

    //MODEL CLASSES
    lateinit var ocr : CustomOCR
    lateinit var tts : CustomTTS
    lateinit var cap : CustomAudioPlayer

    //Initialization
    fun initModel(app: Application, listener: MediaPlayer.OnCompletionListener){
        if(!viewModelInitialied) {
            viewModelScope.launch(newSingleThreadContext("Initialize View Model")) {
                application = app
                //clear temporary files if they exist
                clearDirs(app)
                //isntantiate model classes
                ocr = CustomOCR()
                tts = initTTS()
                cap = CustomAudioPlayer().also { it.createPlayer() }
                Log.i("viewmodel", "instantiated model classes----------------------------------------------------------------")
                withContext(Dispatchers.Main) {
                    setTrackCompletionListener(listener)
                    viewModelInitialied = true
                    ttsInitialized.value = true
                }
            }
        }
        else{
            setTrackCompletionListener(listener)
        }
    }
    suspend fun initTTS() = suspendCancellableCoroutine<CustomTTS> { continuation ->
        val customObjectTTS = CustomTTS()
        customObjectTTS.initTTS(application, object : TextToSpeech.OnInitListener{
            override fun onInit(status: Int) {
                Log.i("ViewModel", "TTS initialized----------------------------------------------------------------")
                continuation.resume(customObjectTTS)
            }
        })
    }

    //Called by Main Activity
    fun photoTaken() = viewModelScope.launch {
        val rotatedImage = async(Dispatchers.IO) {
            return@async photoToAudio()
        }
        withContext(Dispatchers.Main){
            setDisplayImage(rotatedImage.await())
            setPlayerControls(true)
        }
    }

    //Convert image to audio file
    suspend fun photoToAudio() : Bitmap{
        if(!this::imageFile.isInitialized) { throw Error("can't load image file before photo is taken") }
        val (originalImage, rotatedImage, rotation) = ocr.retrieveImageFromFile(imageFile)
        imageFile.delete()
        val text = extractText(originalImage, rotation).text
        tts.textToAudioFile(text, getAudioFile())
        cap.loadAudioFile(audioFile)
        audioFile.delete()
        return rotatedImage
    }

    //Clear Directory
    fun clearDirs(application : Application){
        deleteRecursive(application.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!)
        deleteRecursive(application.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!)
    }
    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) for (child in fileOrDirectory.listFiles()) deleteRecursive(child)
        else{
            fileOrDirectory.delete()
        }
    }

    //LiveData API
    fun setDisplayImage(image : Bitmap){
        takenPhoto.value = image
    }
    fun setPlayerControls(status : Boolean){
        displayPlayerControls.value = status
    }
    fun setInitTTS(status : Boolean){
        ttsInitialized.value = status
    }
    //OCR API
    fun getImageUri() : Uri{
        if(this::imageUri.isInitialized) { return imageUri }
        imageUri = ocr.getImageURI(application, getImageFile())
        return imageUri
    }
    fun getImageFile() : File {
        if(this::imageFile.isInitialized) { return imageFile }
        imageFile = ocr.createImageFile(application)
        return imageFile
    }
    fun extractText(imageMap : Bitmap, rotation : Int) : Text {
        return Tasks.await(ocr.ocrTask(imageMap, rotation))
    }

    //TTS API
    fun getAudioFile() : File{
        if(this::audioFile.isInitialized) { return audioFile }
        audioFile = tts.createAudioFile(application)
        return audioFile
    }
    //CAP API
    fun audioPlayerAction(action : Int) {
        cap.audioPlayerAction(action)
    }
    fun seekTo(position : Int) {
        cap.seekTo(position)
    }
    fun isPlaying() : Boolean{
        return cap.isPlaying()
    }
    fun getPlayerPosition() : Int {
        return cap.getPlayerPosition()
    }
    fun getAudioDuration() : Int {
        Log.i("View Model", cap.getPlayerDuration().toString())
        return cap.getPlayerDuration()
    }
    fun setTrackCompletionListener(listener : MediaPlayer.OnCompletionListener){
        cap.setOnCompleteListener(listener)
        Log.i("mainviewmodel", "set track completed listener")
    }
}