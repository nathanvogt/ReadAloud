package com.example.daa

import android.app.Application
import android.content.Context

class MyApplication : Application() {

    lateinit var context : Context
//    lateinit var tR : TextRecognizer
//    var iL = CustomOCR()
//    lateinit var txtToSpeech : TextToSpeech
//    lateinit var cAP : CustomAudioPlayer
    override fun onCreate() {
        super.onCreate()
    }
//    fun initApp(){
//        if(initialized == null || !initialized){
//            context = applicationContext
//            tR = TextRecognition.getClient()
//            iL = CustomOCR()
//            txtToSpeech = TextToSpeech(context, iL)
//            cAP = CustomAudioPlayer().also{ player -> player.createPlayer()}
//            initialized = true
//        }
//        else{
//            return Unit
//        }
//    }
    companion object {
        private lateinit var context: Context
        val appContext: Context
            get() = context
    }
}