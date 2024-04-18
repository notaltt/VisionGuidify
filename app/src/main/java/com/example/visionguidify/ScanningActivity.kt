package com.example.visionguidify

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.integration.android.IntentIntegrator
import java.util.Locale

class ScanningActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    lateinit var resultQR: TextView
    lateinit var titleQR: TextView
    private var tts: TextToSpeech? = null
    private val RQ_SPEECH_RC = 102
    private var speechInputValue: String? = null
    private var isWaitingForInput: Boolean = false
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
        isWaitingForInput = false
        if(requestCode == RQ_SPEECH_RC && resultCode == Activity.RESULT_OK){
            val result:ArrayList<String>? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            speechInputValue = result?.get(0).toString().uppercase()
        }

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                resultQR.text = result.contents
                checkBarcode(result.contents)
                val cameraIntent = Intent(this@ScanningActivity, CameraActivity::class.java)
                startActivity(cameraIntent)
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

    private fun checkBarcode(input: String) {
        val splitText = input.split(", ")
        if ("VisionGuidify" in splitText) {
            val infoIndex = splitText.indexOf("INFORMATION")
            val directionIndex = splitText.indexOf("DIRECTION")
            val navIndex = splitText.indexOf("NAVIGATION")

            if (infoIndex != -1) {
                val nextWords = splitText.subList(infoIndex + 1, minOf(infoIndex + 4, splitText.size))
                speakText("INFORMATION QR CODE DETECTED")
                nextWords.forEach { word ->
                    speakText(word)
                }
            } else if (navIndex != -1 && navIndex + 1 < splitText.size) {
                val qrLocation = splitText[navIndex + 1]

                val availableDirections = extractDynamicStrings(input)

                speakText("NAVIGATION QR CODE DETECTED")
                speakText("You are currently at $qrLocation")
                speakText("Available directions are $availableDirections")
                delayedSpeakText("Please, choose your direction.")

            } else if (directionIndex != -1) {
                val nextWords = splitText.subList(directionIndex + 1, minOf(directionIndex + 4, splitText.size))
                speakText("DIRECTION QR CODE DETECTED")
                nextWords.forEach { word ->
                    speakText(word)
                }
            } else {
                speakText("Invalid QR CODE")
            }
        } else {
            speakText("UNKNOWN QR CODE")
        }
    }

    private fun delayedSpeakText(text: String, callback: (() -> Unit)? = null) {
        speakText(text)
        Handler(Looper.getMainLooper()).postDelayed({
            askSpeechInput()
        }, 1000) // Delay for 1 second (adjust as needed)
    }


    private fun chooseDirection(input: String) {
        if (speechInputValue != null) {
            handleUserInput(input, speechInputValue!!)
        } else {
            // Handle case where speechInputValue is still null
            askSpeechInput()
        }
    }

    private fun handleUserInput(input: String, speechInputValue: String?) {
        val choicedDirection = extractDirectionInstructions(input, speechInputValue!!)
        speakText("You choose $speechInputValue, here is the instruction $choicedDirection")
    }


    private fun askSpeechInput() {
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            Toast.makeText(this, "speech recognition is not available", Toast.LENGTH_SHORT).show()
            isWaitingForInput = false
        }else{
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Something!")
            startActivityForResult(i, RQ_SPEECH_RC)
        }
    }

    private fun extractDynamicStrings(text: String): List<String> {
        val regex = Regex("""DIRECTION: ([A-Z]+)""")
        val matches = regex.findAll(text)

        return matches.mapNotNull { it.groups[1]?.value }.toList()
    }

    private fun extractDirectionInstructions(text: String, direction: String): List<String> {
        val regex = Regex("""DIRECTION: $direction, \[([a-zA-Z, ]+)\]""")
        val match = regex.find(text)

        return match?.groups?.get(1)?.value?.split(", ") ?: emptyList()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }
}
