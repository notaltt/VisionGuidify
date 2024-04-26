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
import android.view.KeyEvent
import android.widget.Button
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
    lateinit var button: Button
    private var qrCodeResult: String? = null
    private lateinit var availableDirections: List<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)

        // Initialize QR code scanner
        startScanning()
        resultQR = findViewById(R.id.resultQR)
        titleQR = findViewById(R.id.typeQR)
        tts = TextToSpeech(this, this)
        button = findViewById(R.id.speechButton)

//        button.setOnClickListener {
//            askSpeechInput()
//        }
    }

    private fun startScanning() {
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false) // Allow both portrait and landscape scanning
        integrator.setBeepEnabled(false) // Disable beep sound when scanning
        integrator.setPrompt("Scan QR code") // Set a prompt message for the scanner
        integrator.initiateScan() // Start QR code scanning
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && !isWaitingForInput) {
            // Perform your action here
            askSpeechInput()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == RQ_SPEECH_RC && resultCode == Activity.RESULT_OK){
            val result:ArrayList<String>? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            speechInputValue = result?.get(0).toString().uppercase()
            handleUserInput(qrCodeResult!!, speechInputValue)
        }

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                resultQR.text = result.contents
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
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    private fun checkBarcode(input: String) {
        val splitText = input.split(", ")
        val cameraIntent = Intent(this@ScanningActivity, MainActivity::class.java)
        qrCodeResult = input
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

                startActivity(cameraIntent)

            } else if (navIndex != -1 && navIndex + 1 < splitText.size) {
                val qrLocation = splitText[navIndex + 1]

                availableDirections = extractDynamicStrings(input)

                speakText("NAVIGATION QR CODE DETECTED")
                speakText("You are currently at $qrLocation")
                speakText("Available directions are $availableDirections")
                speakText("Please, choose your direction.")

            } else if (directionIndex != -1) {
                val nextWords = splitText.subList(directionIndex + 1, minOf(directionIndex + 4, splitText.size))
                speakText("DIRECTION QR CODE DETECTED")
                nextWords.forEach { word ->
                    speakText(word)
                }

                startActivity(cameraIntent)

            } else {
                speakText("Invalid QR CODE")
                startScanning()
            }
        } else {
            speakText("UNKNOWN QR CODE")
            startScanning()
        }
    }


    private fun handleUserInput(input: String, speechInputValue: String?) {
        val choicedDirection = extractDirectionInstructions(input, speechInputValue!!)
        val firstDirection = choicedDirection.firstOrNull()
        if (firstDirection != null) {
            speakText("You choose $speechInputValue, here is the instruction $choicedDirection")
            speakText("To start, please go $firstDirection")

            val navigationIntent = Intent(this@ScanningActivity, NavigationActivity::class.java)
            navigationIntent.putExtra("direction", choicedDirection.joinToString(", "))
            navigationIntent.putExtra("location", speechInputValue)
            startActivity(navigationIntent)
        } else {
            speakText("You choose $speechInputValue, sorry I can't find it in the available directions.")
        }
    }


    private fun askSpeechInput() {
        if(!SpeechRecognizer.isRecognitionAvailable(this)){
            Toast.makeText(this, "speech recognition is not available", Toast.LENGTH_SHORT).show()
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
