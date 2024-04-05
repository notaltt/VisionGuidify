package com.example.visionguidify

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.TextView
import com.google.zxing.integration.android.IntentIntegrator
import java.util.Locale

class ScanningActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    lateinit var resultQR: TextView
    lateinit var titleQR: TextView
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)

        // Initialize QR code scanner
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false) // Allow both portrait and landscape scanning
        integrator.setBeepEnabled(false) // Disable beep sound when scanning
        integrator.setPrompt("Scan QR code") // Set a prompt message for the scanner
        integrator.initiateScan() // Start QR code scanning
        resultQR = findViewById(R.id.resultQR)
        titleQR = findViewById(R.id.typeQR)
        tts = TextToSpeech(this, this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                resultQR.text = result.contents
//                speakText(result.contents) // Speak the scanned text
                checkBarcode(result.contents)
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
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    @SuppressLint("SetTextI18n")
    private fun checkBarcode(input: String) {
        when {
            "INFORMATION" in input -> {
                Log.e("BARCODE", "INFORMATION BARCODE DETECTED")
                speakText("INFORMATION BARCODE DETECTED")
                titleQR.text = "INFORMATION BARCODE"
            }
            "NAVIGATION" in input -> {
                Log.e("BARCODE", "NAVIGATION BARCODE DETECTED")
                speakText("NAVIGATION BARCODE DETECTED")
                titleQR.text = "NAVIGATION BARCODE"
            }
            "DIRECTION" in input -> {
                Log.e("BARCODE", "DIRECTION BARCODE DETECTED")
                speakText("DIRECTION BARCODE DETECTED")
                titleQR.text = "DIRECTION BARCODE"
            }
            else -> {
                Log.e("BARCODE", "Unknown barcode type")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}
