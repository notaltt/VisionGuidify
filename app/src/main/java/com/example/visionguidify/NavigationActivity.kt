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
    private var source:String? = null
//    private var arrayList: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        list = findViewById(R.id.instructionList)
        directionList = intent.getStringExtra("direction")
        locationNav = intent.getStringExtra("location")
        source = intent.getStringExtra("source")

        tts = TextToSpeech(this, this)

//        directionList?.let {
//            arrayList = ArrayList(it.split("\n"))
//            list.text = arrayList?.joinToString("\n")
//        }

        if(source == "MainActivity"){
            startQRScanner()
        }else{
            mainIntent()
        }
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val resultStringList = result.contents
                val splitResult = resultStringList.split(", ")
                val thirdElement = splitResult[2]
                val splitDirection = directionList?.split(", ")?.toMutableList()
                splitDirection?.removeAt(0)
                list.text = splitDirection.toString()

                for (item in splitResult) {
                    if (splitDirection?.contains(item) == true) {
                        splitDirection.remove(item)
                        speakText(item)
                        mainIntent()
                    }
                }

                if (splitDirection != null) {
                    if (splitDirection.isEmpty()) {
                        speakText("YOU ARE NEAR $locationNav, LOOK FOR INFORMATION QR CODE")
                        mainIntent()
                    }
                }

                if(thirdElement == locationNav) {
                    speakText("YOU ARRIVED AT $locationNav")
                    val intentCamera = Intent(this@NavigationActivity, MainActivity::class.java)
                    startActivity(intentCamera)
                }else{
                    speakText(thirdElement)
                }

            //                while (splitDirection?.isNotEmpty() == true) {
            //                    val removedValues = splitDirection.filter { direction ->
            //                        splitResult.any { result -> result == direction }
            //                    }
            //                    speakText(removedValues.toString())
            //                    splitDirection.removeAll(removedValues)
            //
            //                    if (splitDirection.isEmpty()) {
            //                        startQRScanner("Look for INFORMATION QR Code")
            //                        if (locationNav in splitResult) {
            //                            speakText("You arrived at $locationNav")
            //                            val intentCamera = Intent(this@NavigationActivity, MainActivity::class.java)
            //                            startActivity(intentCamera)
            //                        }
            //                    } else {
            //                        startQRScanner("Look for DIRECTION QR Code")
            //                    }
            //                }

                }
            } else {
                Log.e("QR Result", "No content found")
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

    fun mainIntent(){
        val intent = Intent(this@NavigationActivity, MainActivity::class.java)
        intent.putExtra("source", "NavigationActivity")
        intent.putExtra("direction", directionList)
        intent.putExtra("location", locationNav)
        startActivity(intent)
    }

    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    private fun startQRScanner(prompt: String = "Start QR Scan") {
        val integrator = IntentIntegrator(this)
        integrator.setOrientationLocked(false)
        integrator.setBeepEnabled(false)
        integrator.setPrompt(prompt)
        integrator.initiateScan()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
    }

}