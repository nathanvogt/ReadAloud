package com.example.daa.data.model

import android.media.MediaPlayer
import android.provider.MediaStore
import java.io.File

val CAP_PLAY = 0
val CAP_PAUSE = 1
val CAP_REW = 2
val CAP_FF = 3

class CustomAudioPlayer {
    private lateinit var systemPlayer : MediaPlayer
    private var playerActive = false
    val SKIP_AMOUNT = 5000

    //Initialization
    fun createPlayer(){
        if (!playerActive){
            systemPlayer = MediaPlayer()
        }
        else{
            systemPlayer.reset()
        }
        playerActive = true
    }
    fun loadAudioFile(audioFile : File){
        systemPlayer.reset()
        systemPlayer.setDataSource(audioFile.absolutePath)
        systemPlayer.prepare()
    }
    fun setOnCompleteListener(listener : MediaPlayer.OnCompletionListener){
        systemPlayer.setOnCompletionListener(listener)
    }

    //Control Audio Player
    fun audioPlayerAction(action : Int) {
        when(action){
            CAP_PLAY -> play()
            CAP_PAUSE -> pause()
            CAP_FF -> fastforward()
            CAP_REW -> rewind()
        }
    }
    fun playPause(){
        if(systemPlayer.isPlaying){
            pause()
        }
        if(!systemPlayer.isPlaying){
            play()
        }
    }
    fun pause(){
        if(systemPlayer.isPlaying){
            systemPlayer.pause()
        }
    }
    fun play(){
        if(!systemPlayer.isPlaying){
            systemPlayer.start()
        }
    }
    fun rewind(){
        val p = systemPlayer.currentPosition
        var newP = p - SKIP_AMOUNT
        if(newP < 0){newP = 0}
        systemPlayer.seekTo(newP)
    }
    fun fastforward() {
        var newP = systemPlayer.currentPosition + SKIP_AMOUNT
        val duration = systemPlayer.duration
        if(newP > duration){
            newP = duration
        }
        systemPlayer.seekTo(newP)
    }
    fun seekTo(position : Int) {
        systemPlayer.seekTo(position)
    }

    //Audio Player Information
    fun isPlaying() : Boolean{
        return systemPlayer.isPlaying
    }
    fun getPlayerPosition() : Int {
        return systemPlayer.currentPosition
    }
    fun getPlayerDuration() : Int {
        return systemPlayer.duration
    }
    fun destroy(){
        systemPlayer.release()
    }
}