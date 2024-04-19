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
    private var arrayList: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        list = findViewById(R.id.instructionList)
        directionList = intent.getStringExtra("direction")
        tts = TextToSpeech(this, this)

        directionList?.let {
            arrayList = ArrayList(it.split("\n"))
            list.text = arrayList?.joinToString("\n")
        }

        startQRScanner()
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                // Convert result.contents to ArrayList<String>
                val resultStringList = arrayListOf(result.contents)
                resultStringList.addAll(arrayList!!.subList(0, minOf(2, arrayList!!.size)))

                speakText("DIRECTION QR CODE DETECTED") // Speak the detection message

                arrayList?.let {
                    for (item in it) {
                        if (resultStringList.contains(item)) {
                            // Speak the scanned result
                            if (tts != null && tts!!.isSpeaking) {
                                tts!!.stop() // Stop speaking if already speaking
                            }
                            speakText(item)

                            it.remove(item) // Remove the matched element from the list
                            list.text = it.joinToString("\n") // Update the TextView with the updated list

                            if (it.isEmpty()) {
                                // Navigate back to CameraActivity when the list is empty
                                val intent = Intent(this, CameraActivity::class.java)
                                startActivity(intent)
                            } else {
                                // Continue scanning for QR codes
                                startQRScanner()
                            }
                            break // Exit the loop after processing the match
                        }
                    }

                    if (!it.isEmpty()) {
                        // If no match found in the list, prompt for another scan
                        speakText("QR CODE NOT RECOGNIZED")
                        startQRScanner()
                    }
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

    private fun startQRScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false) // Allow both portrait and landscape scanning
        integrator.setBeepEnabled(false) // Disable beep sound when scanning
        integrator.setPrompt("LOOK FOR DIRECTION QR CODE") // Set a prompt message for the scanner
        integrator.initiateScan() // Start QR code scanning
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }

}