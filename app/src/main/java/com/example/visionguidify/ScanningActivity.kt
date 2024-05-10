package com.example.visionguidify

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.controls.ControlsProviderService.TAG
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

class ScanningActivity : AppCompatActivity(), TextToSpeech.OnInitListener, BluetoothManager.BluetoothMessageListener {
    lateinit var resultQR: TextView
    lateinit var titleQR: TextView
    private var tts: TextToSpeech? = null
    private val RQ_SPEECH_RC = 102
    private var speechInputValue: String? = null
    private var isWaitingForInput: Boolean = false
    lateinit var button: Button
    private var qrCodeResult: String? = null
    private lateinit var availableDirections: List<String>
    private lateinit var bluetoothManager: BluetoothManager<Any?> // Add this line

    private var isQRCodeDetected: Boolean = false
    private val bluetoothListener = ScanningActivityBluetoothListener()

    private val handler1 = Handler(Looper.getMainLooper())
    private var isScanning = false
    private var isTtsRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning)

        // Initialize BluetoothManager
        bluetoothManager = BluetoothManager(this, bluetoothListener)
        bluetoothManager.setMessageListener(this)

        // Initialize QR code scanner
        startScanning()
        resultQR = findViewById(R.id.resultQR)
        titleQR = findViewById(R.id.typeQR)
        tts = TextToSpeech(this, this)
        button = findViewById(R.id.speechButton)

        button.setOnClickListener {
            askSpeechInput()
        }

        if (isScanning){
//            startTtsWithInterval("Slowly turn right and left, to look for the QR Code")
            speakText("TEST SCANNING")
        }
    }

    private fun startTtsWithInterval(message: String) {
        if (!isTtsRunning) {
            isTtsRunning = true
            speakText(message)

            handler1.postDelayed({
                isTtsRunning = false
            }, 10000) // 10-second interval
        }
    }


    override fun onBluetoothMessageReceived(message: String) {
        if (isQRCodeDetected) {
            // Handle Bluetooth messages only after QR code detection
            if (message.trim() == "OPEN") {
                speakText("MICROPHONE IS OPEN")
                askSpeechInput()
            }
        } else {
            // Optionally, you can queue the messages or ignore them until QR code detection
            // Or you can handle them differently based on your app's requirements
            startTtsWithInterval("Slowly turn right and left, to look for the QR Code")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RQ_SPEECH_RC && resultCode == Activity.RESULT_OK) {
            val result: ArrayList<String>? = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            speechInputValue = result?.get(0).toString().uppercase()
            handleUserInput(qrCodeResult!!, speechInputValue)
        }

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                resultQR.text = result.contents
                checkBarcode(result.contents)
                // Set the flag to indicate QR code detection
                isQRCodeDetected = true
                isScanning = false
            } else {
                Log.e("QR Result", "No content found")
            }
        }
    }

    inner class ScanningActivityBluetoothListener : BluetoothManager.BluetoothConnectionListener {
        override fun onBluetoothConnected() {
            speakText("Bluetooth connected.")
            // Add any additional logic you want to execute upon Bluetooth connection
        }
    }

    private fun startScanning() {
        isScanning = true
        try {
            val integrator = IntentIntegrator(this)
            integrator.setOrientationLocked(false) // Allow both portrait and landscape scanning
            integrator.setBeepEnabled(false) // Disable beep sound when scanning
            integrator.setPrompt("Scan QR code") // Set a prompt message for the scanner
            integrator.initiateScan() // Start QR code scanning
        } catch (e: Exception) {
            // Handle any exceptions that occur during QR code scanning
            Log.e(TAG, "Error starting QR code scanning: ${e.message}")
            Toast.makeText(this, "Error starting QR code scanning", Toast.LENGTH_SHORT).show()

            // Handle Bluetooth disconnection or input stream issues
            if (bluetoothManager.isBluetoothConnected()) {
                Log.e(TAG, "Bluetooth connection is active")
            } else {
                Log.e(TAG, "Bluetooth connection is not active")
                // You can attempt to reconnect to the Bluetooth device here if needed
                // Example: bluetoothManager.connectToDevice()
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

                bluetoothManager.connectToDevice()

            } else if (navIndex != -1 && navIndex + 1 < splitText.size) {
                val qrLocation = splitText[navIndex + 1]

                availableDirections = extractDynamicStrings(input)

                speakText("NAVIGATION QR CODE DETECTED")
                speakText("You are currently at $qrLocation")
                speakText("Available directions are $availableDirections")
                speakText("Please, choose your direction.")

                bluetoothManager.connectToDevice()

            } else if (directionIndex != -1) {
                val nextWords = splitText.subList(directionIndex + 1, minOf(directionIndex + 4, splitText.size))
                speakText("DIRECTION QR CODE DETECTED")
                nextWords.forEach { word ->
                    speakText(word)
                }

                // Start Bluetooth connection after scanning valid QR code and initializing TTS
                bluetoothManager.connectToDevice()

                startActivity(cameraIntent)

            } else {
                speakText("Invalid QR CODE")
                val intentCamera = Intent(this@ScanningActivity, MainActivity::class.java)
                startActivity(intentCamera)
            }
        } else {
            speakText("UNKNOWN QR CODE")
            val intentCamera = Intent(this@ScanningActivity, MainActivity::class.java)
            startActivity(intentCamera)
        }
    }


    private fun handleUserInput(input: String, speechInputValue: String?) {

        var commonValue: String = ""

        speechInputValue?.split("\\s+".toRegex())?.forEach { word ->
            if (word in availableDirections) {
                commonValue = word
                return@forEach
            }
        }

        val choicedDirection = extractDirectionInstructions(input, commonValue)
        val firstDirection = choicedDirection.firstOrNull()

        if (firstDirection != null) {
            speakText("You choose $commonValue, here is the instruction $choicedDirection")
            speakText("To start, please go $firstDirection")

            val navigationIntent = Intent(this@ScanningActivity, NavigationActivity::class.java)
            navigationIntent.putExtra("direction", choicedDirection.joinToString(", "))
            navigationIntent.putExtra("location", commonValue)
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
