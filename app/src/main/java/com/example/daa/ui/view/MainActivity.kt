package com.example.daa.ui.view

import android.app.Activity
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.daa.R
import com.example.daa.data.model.CAP_FF
import com.example.daa.data.model.CAP_PAUSE
import com.example.daa.data.model.CAP_PLAY
import com.example.daa.data.model.CAP_REW
import com.example.daa.ui.viewmodel.MainViewModel
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.*


val REQUEST_IMAGE_CAPTURE = 1
val REQUEST_IMAGE_CROP = 2
val OPEN_IMAGE = 3
val UTTERANCE_ID = "DAA_TTS"

class MainActivity : AppCompatActivity() {

    var previousAction = 0

    //UI Widgets
    lateinit var imageView: ImageView
    lateinit var takePhotoButton : Button
    lateinit var openPhotoButton : Button
    lateinit var bt_play_pause : ImageButton
    lateinit var bt_ff : ImageButton
    lateinit var bt_rew : ImageButton
    lateinit var seekBar : SeekBar
    lateinit var loadingSpinner : View

    //SeekBar Updater
    lateinit var seekBarUpdaterJob : Job
    var previouslyPlayed = false

    //View Model
    lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeButtons()


        viewModel = ViewModelProvider(this).get(MainViewModel()::class.java)
                .also { it.initModel(application,
                        MediaPlayer.OnCompletionListener { completedTrack() })}

        initializeObservers()
    }

    //initialization
    fun initializeButtons(){
        takePhotoButton = findViewById<Button>(R.id.takePhotoButton).also{ butt ->
            butt.visibility = View.INVISIBLE
        }
        openPhotoButton = findViewById<Button>(R.id.openPhotoButton).also{ butt ->
            butt.visibility = View.INVISIBLE
        }
        imageView = findViewById(R.id.image_display)
        bt_play_pause = findViewById<ImageButton>(R.id.btPlayPause).also{ butt -> butt.visibility = View.INVISIBLE}
        setPauseButton(true)
        bt_ff = findViewById<ImageButton>(R.id.ffButton).also{ butt -> butt.visibility = View.INVISIBLE}
        bt_rew = findViewById<ImageButton>(R.id.rewButton).also{ butt -> butt.visibility = View.INVISIBLE}
        loadingSpinner = findViewById(R.id.loadingPanel)
        seekBar = findViewById<SeekBar>(R.id.seekBar).also{ butt -> butt.visibility = View.INVISIBLE }.also {
            it.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    //No Need
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    previouslyPlayed = viewModel.isPlaying()
                    pauseAudio()
                    setSeekBarUpdater(false)
                }
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.seekTo(seekBar!!.progress)
                    if(previouslyPlayed) {
                        playAudio()
                        setSeekBarUpdater(true)
                    }
                }
            })
        }
    }
    fun initializeObservers(){
        //when TTS is initialized
        viewModel.ttsInitialized.observe(this, Observer<Boolean>{
            if(it){
                setTakePhotoVisibility(true)
                loadingSpinner.visibility = View.GONE
            }
        })
        viewModel.displayPlayerControls.observe(this, Observer<Boolean> {
            if(it){
                loadingSpinner.visibility = View.GONE
                setPlayerVisibility(true, viewModel.getAudioDuration())
                setTakePhotoVisibility(true)
                imageView.setImageBitmap(viewModel.takenPhoto.value)
                if(viewModel.isPlaying()){
                    setSeekBarUpdater(true)
                }
                if(!viewModel.isPlaying()){
                    setPauseButton(false)
                }
            }
        })
    }

    //photo button calls
    fun takeImage(view: View) {
        startPicIntent()
    }
    fun openImage(view : View){
        startOpenPicIntent()
    }
    fun startCropper(uri : Uri){
        // start cropping activity for pre-acquired image saved on the device
        CropImage.activity(uri)
                .setInitialCropWindowPaddingRatio(0.toFloat())
                .start(this)
    }

    //start photo conversion process
    fun startPicIntent(){
        pauseAudio()
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, viewModel.createImageUri())
        takePictureIntent.putExtra("return-data", true)
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
    }
    fun startOpenPicIntent(){
        //open image from gallery
        val i = Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(i, OPEN_IMAGE)
    }
    fun convertPhoto(){
        viewModel.setInitTTS(false)
        viewModel.displayPlayerControls.value = false
        imageView.setImageDrawable(null)
        setTakePhotoVisibility(false)
        setPlayerVisibility(false)
        loadingSpinner.visibility = View.VISIBLE
        seekBar.setProgress(0)
        viewModel.photoTaken()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == REQUEST_IMAGE_CAPTURE){
            if(resultCode == Activity.RESULT_OK){
                //crop the taken photo
                previousAction = 0
                startCropper(viewModel.getImageUri())
            }
            if(resultCode != Activity.RESULT_OK && resultCode != Activity.RESULT_CANCELED){
                val toast = Toast.makeText(this, "Could not capture image", Toast.LENGTH_SHORT)
                toast.show()
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            //successful crop
            if(resultCode == Activity.RESULT_OK){
                val result = CropImage.getActivityResult(data)
                viewModel.setImageURs(result.uri)
                convertPhoto()
            }
            //if cancelled crop
            if(resultCode == 0){
                when(previousAction){
                    0 -> startPicIntent()
                    1 -> startOpenPicIntent()
                }
            }
        }
        if(requestCode == OPEN_IMAGE){
            //successfully opened image
            if(resultCode == Activity.RESULT_OK){
                val uri = data?.data
                if(uri != null){
                    viewModel.setImageURs(uri)
                    previousAction = 1
                    startCropper(uri)
                }
            }
        }
    }

    //ui widget visibility controllers
    fun setTakePhotoVisibility(status : Boolean){
        if(status){
            takePhotoButton.visibility = View.VISIBLE
            openPhotoButton.visibility = View.VISIBLE
        }
        if(!status){
            takePhotoButton.visibility = View.INVISIBLE
            openPhotoButton.visibility = View.INVISIBLE
        }
    }
    fun setPlayerVisibility(status : Boolean, duration : Int? = null){
        if(status){
            bt_play_pause.visibility = View.VISIBLE
            bt_ff.visibility = View.VISIBLE
            bt_rew.visibility = View.VISIBLE
            if(duration != null){ seekBar.max = duration }
            seekBar.visibility = View.VISIBLE
        }
        if(!status){
            bt_play_pause.visibility = View.INVISIBLE
            bt_ff.visibility = View.INVISIBLE
            bt_rew.visibility = View.INVISIBLE
            seekBar.visibility = View.INVISIBLE
        }
    }
    fun setPauseButton(status : Boolean){
        if(status){
            bt_play_pause.setImageResource(R.drawable.ic_media_pause)
        }
        else{
            bt_play_pause.setImageResource(R.drawable.ic_media_play)
        }
    }

    //audio player controls
    fun playPause(view: View){
        val isPlaying = viewModel.isPlaying()
        if(isPlaying){
            pauseAudio()
        }
        if(!isPlaying){
            playAudio()
        }
    }
    fun playAudio(){
        setPauseButton(true)
        viewModel.audioPlayerAction(CAP_PLAY)
        setSeekBarUpdater(true)
    }
    fun pauseAudio(){
        setPauseButton(false)
        viewModel.audioPlayerAction(CAP_PAUSE)
        setSeekBarUpdater(false)
    }
    fun ff(view : View){
        viewModel.audioPlayerAction(CAP_FF)
        viewModel.audioPlayerAction(CAP_PLAY)
        if(viewModel.isPlaying()){
            setPauseButton(true)
            setSeekBarUpdater(true)
        }
    }
    fun rew(view : View){
        viewModel.audioPlayerAction(CAP_REW)
        playAudio()
        setSeekBarUpdater(true)
    }

    //listener callbacks
    fun completedTrack(){
        Log.i("main activity", "Completed playing audio callback")
        setPauseButton(false)
        setSeekBarUpdater(false)
        seekBar.setProgress(viewModel.getAudioDuration())
    }

    //SeekBar Updaters
    fun updateSeekBar() = GlobalScope.launch(viewModel.updateThread) {
        while(isActive && viewModel.isPlaying()){
            val position = viewModel.getPlayerPosition()
            withContext(Dispatchers.Main) {
                seekBar.setProgress(position)
            }
//            TODO("Dynamic delay amount depending on audio file length")
            delay(50)
        }
    }
    fun setSeekBarUpdater(status : Boolean) {
        //START THE UPDATER
        if(status){
            //if not initialized or not active
            if(!this::seekBarUpdaterJob.isInitialized || !seekBarUpdaterJob.isActive){
                seekBarUpdaterJob = updateSeekBar().also { it.start() }
                return Unit
            }

            //check if already running
            if(seekBarUpdaterJob.isActive){ return Unit }
        }

        //STOP THE UPDATER
        if(!status){
            //check if not initialized or not active
            if(!this::seekBarUpdaterJob.isInitialized || !seekBarUpdaterJob.isActive){ return Unit }

            //if running
            if(seekBarUpdaterJob.isActive){
                seekBarUpdaterJob.cancel()
                return Unit
            }
        }
    }

}
