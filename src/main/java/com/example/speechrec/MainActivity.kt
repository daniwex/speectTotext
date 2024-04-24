package com.example.speechrec


import android.Manifest.permission
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val TAG = "f"
    private var started = true
    private lateinit var imageButton: ImageButton
    private lateinit var buttonPlay: Button
    private lateinit var buttonStop: Button
    private lateinit var textView: TextView
    private lateinit var textView4: TextView
    private val REQUEST_AUDIO_PERMISSION_CODE = 1
    lateinit var speechRecognizer: SpeechRecognizer
    private var permissionToRecord = false
    private lateinit var mRecorder: MediaRecorder
    private var checkPress: Int = 0
    private var mPlayer: MediaPlayer? = null

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        textView4 = findViewById(R.id.textView4)
        textView = findViewById(R.id.textView2)
        imageButton = findViewById(R.id.imageButton)
        buttonPlay = findViewById(R.id.buttonPlay)
        buttonStop = findViewById(R.id.buttonStop)

        imageButton.setOnClickListener {
            val recognizerIntent: Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
            }
            recognizerIntent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR")
            recognizerIntent.putExtra("android.speech.extra.GET_AUDIO", true)
            try {
                startRecording(recognizerIntent)
            } catch (e: ActivityNotFoundException) {
                Log.d(TAG, e.toString())
            }
        }

        buttonPlay.setOnClickListener { playAudio() }
        buttonStop.setOnClickListener {
            pausePlaying()
        }
    }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            val result: ArrayList<String>? =
                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            textView4.text = result?.get(0)
            try {
                var textFile: FileOutputStream? = null

                textFile = FileOutputStream(getTranscriptionPath())
                textFile.write(result?.get(0)?.toByteArray())
                textFile.close()
                textFile = null

            } catch (e: FileNotFoundException) {
                Log.d(TAG, e.toString())
            }

            val audioU = data?.data
            Log.d(TAG,audioU.toString())
            val contentResolver = contentResolver
            try {
                var filestream = audioU?.let { contentResolver.openInputStream(it) }
                var audioOut: FileOutputStream? = FileOutputStream(getFilePath())
                val buffer = ByteArray(99999)
                var read: Int
                if (filestream != null) {
                    while (filestream.read(buffer).also { read = it } != -1) {
                        audioOut!!.write(buffer, 0, read)
                    }
                }else{
                    Log.d(TAG, "error")
                }
                filestream?.close()
                filestream = null
                audioOut!!.flush()
                audioOut.close()
                audioOut = null

            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            super.onActivityResult(requestCode, resultCode, data)
        }


        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            when (requestCode) {
                this@MainActivity.REQUEST_AUDIO_PERMISSION_CODE -> if (grantResults.isNotEmpty()) {
                    permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val permissionToStore = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (permissionToRecord && permissionToStore) {
                        Toast.makeText(applicationContext, "Permission Granted", Toast.LENGTH_LONG)
                            .show()
                    } else {
                        Toast.makeText(applicationContext, "Permission Denied", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }


        private fun startRecording(speechRecognizerIntent: Intent) {
//            if(checkPermissions()){
            val text = "Tap to end recording"
            textView.text = text
            startActivityForResult(speechRecognizerIntent, REQUEST_AUDIO_PERMISSION_CODE)

//            }else{
//                requestPermissions()
//            }
        }

        private fun checkPermissions(): Boolean {
            // check permission
            val result = ContextCompat.checkSelfPermission(
                applicationContext,
                permission.WRITE_EXTERNAL_STORAGE
            )
            val result1 =
                ContextCompat.checkSelfPermission(applicationContext, permission.RECORD_AUDIO)
            return result == PackageManager.PERMISSION_GRANTED && result1 == PackageManager.PERMISSION_GRANTED
        }

        private fun requestPermissions() {
            // request the permission for audio recording and storage.
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf<String>(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO), REQUEST_AUDIO_PERMISSION_CODE
            )

        }

        private fun getFilePath(): String {
            val contextWrapper = ContextWrapper(this)
            val musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            val file = File(musicDirectory, "audio_sample" + ".amr")
            return file.path
        }

    private fun getTranscriptionPath(): String? {
        val contextWrapper = ContextWrapper(this)
        val docDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        val file = File(docDirectory, "audio_transcription" + ".txt")
        return file.path
    }
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private fun playAudio() {
            checkPress = 1
            //use mPlayer class to playback a recording

            mPlayer = MediaPlayer()

            try {

                // data source will be our file name
                mPlayer!!.setDataSource(getFilePath())
                mPlayer!!.prepare()
                mPlayer!!.setOnPreparedListener(OnPreparedListener { playerM -> playerM.start() })
            } catch (e: IOException) {
                Log.e("TAG", "prepare() failed")
            }

        }

        fun pausePlaying() {
            // release the media player class and pause the playing of our recorded audio.
            if (checkPress == 1) {
                mPlayer?.release()
                mPlayer = null

                checkPress = 0
            } else {
                return
            }
        }
    }



