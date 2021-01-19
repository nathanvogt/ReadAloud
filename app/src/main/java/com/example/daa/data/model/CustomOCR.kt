package com.example.daa.data.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CustomOCR {
    private val textRecognizer = TextRecognition.getClient()
    private lateinit var imageFile: File

    init {

    }

    fun ocrTask(imageMap: Bitmap, rotation : Int) : Task<Text>{
        val inputImage = InputImage.fromBitmap(imageMap, rotation)
        return textRecognizer.process(inputImage)
    }
    fun createImageFile(context: Context) : File {
        // Create an image file
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        )
    }
    fun retrieveImageFromFile(file : File) : Triple<Bitmap, Bitmap, Int>{
        val imagePath = file.absolutePath
        val originalImageBitmap = BitmapFactory.decodeFile(imagePath)
        val rotation = getImageRotation(imagePath)
        val rotatedImageBitmap = rotateImage(originalImageBitmap, rotation)
        return Triple(originalImageBitmap, rotatedImageBitmap, rotation)
    }
    fun getImageURI(context: Context, file: File) : Uri {
        return FileProvider.getUriForFile(
                context,
                "com.example.daa.fileprovider",
                file
        )
    }
    private fun getImageRotation(imagePath : String): Int {
        val exifInterface = ExifInterface(imagePath)
        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, 69)
        when(orientation){
            ExifInterface.ORIENTATION_NORMAL -> return 0
            ExifInterface.ORIENTATION_ROTATE_90 -> return 90
            ExifInterface.ORIENTATION_ROTATE_180 -> return 180
            ExifInterface.ORIENTATION_ROTATE_270 -> return 270
        }
        return orientation
    }
    private fun rotateImage(img: Bitmap, degree: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        return rotatedImg
    }
}