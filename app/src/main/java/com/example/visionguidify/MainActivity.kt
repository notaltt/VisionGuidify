package com.example.visionguidify

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button = findViewById(R.id.allowButton)
        button.setOnClickListener {
            checkAndRequestPermissions()
        }

        if (arePermissionsGranted()) {
            startCameraActivity()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val permissionMicrophone = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)

        if (permissionCamera != PackageManager.PERMISSION_GRANTED || permissionMicrophone != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ),
                101
            )
        } else {
            startCameraActivity()
        }
    }

    private fun arePermissionsGranted(): Boolean {
        val permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val permissionMicrophone = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        return permissionCamera == PackageManager.PERMISSION_GRANTED && permissionMicrophone == PackageManager.PERMISSION_GRANTED
    }

    private fun startCameraActivity() {
        val intent = Intent(this, CameraActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && arePermissionsGranted()) {
            startCameraActivity()
        }
    }
}
