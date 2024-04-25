package com.example.visionguidify

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import com.google.zxing.integration.android.IntentIntegrator
import java.util.Locale

class NavigationActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var list: TextView
    private var tts: TextToSpeech? = null
    private var directionList: String? = null
    private var locationNav: String? = null
//    private var arrayList: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        list = findViewById(R.id.instructionList)
        directionList = intent.getStringExtra("direction")
        locationNav = intent.getStringExtra("location")

        tts = TextToSpeech(this, this)

//        directionList?.let {
//            arrayList = ArrayList(it.split("\n"))
//            list.text = arrayList?.joinToString("\n")
//        }

        startQRScanner()
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val resultStringList = result.contents
                val splitResult = resultStringList.split(", ")
                val splitDirection = directionList?.split(", ")?.toMutableList()
                splitDirection?.removeAt(0)
                list.text = splitDirection.toString()

                val removedValues = splitDirection?.let { directionList ->
                    directionList.filter { direction ->
                        splitResult.any { result -> result == direction }
                    }.also { removedList ->
                        splitDirection.removeAll { removedList.contains(it) }
                    }
                }

                speakText(removedValues.toString())

                if (splitDirection.isNullOrEmpty()) {
                    startQRScanner("Look for INFORMATION QR Code")
                    if (locationNav in splitResult) {
                        speakText("You arrived at $locationNav")
                        val intentCamera = Intent(this@NavigationActivity, MainActivity::class.java)
                        startActivity(intentCamera)
                    }
                } else {
                    startQRScanner("Look for DIRECTION QR Code")
                }

            } else {
                Log.e("QR Result", "No content found")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "The Language not supported!")
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    private fun startQRScanner(prompt: String = "Start QR Scan") {
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false) // Allow both portrait and landscape scanning
        integrator.setBeepEnabled(false) // Disable beep sound when scanning
        integrator.setPrompt(prompt) // Set a prompt message for the scanner
        integrator.initiateScan() // Start QR code scanning
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }

}