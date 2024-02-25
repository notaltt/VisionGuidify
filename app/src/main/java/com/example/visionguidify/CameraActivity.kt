package com.example.visionguidify

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.TextView
import com.example.visionguidify.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CameraActivity : AppCompatActivity() {

    lateinit var textureView: TextureView
    lateinit var textView: TextView
    lateinit var cameraManager: CameraManager
    lateinit var handler: Handler
    lateinit var cameraDevice: CameraDevice
    lateinit var bitmap: Bitmap
    lateinit var byteBuffer: ByteBuffer
    lateinit var model: Model
    lateinit var imageProcessor: ImageProcessor
    val intValues = IntArray(224 * 224)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        model = Model.newInstance(this)
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper);

        textureView = findViewById(R.id.textureView)
        textView = findViewById(R.id.detectedClasses)
        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                openCamera();
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = textureView.bitmap!!

                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.UINT8)

                // Resize the bitmap to match the TensorBuffer dimensions
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, false)

                // Load the resized bitmap into the input TensorBuffer
                val byteBuffer = ByteBuffer.allocateDirect(1 * 224 * 224 * 3)
                byteBuffer.order(ByteOrder.nativeOrder())
                resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)
                var pixel = 0
                for (i in 0 until 224) {
                    for (j in 0 until 224) {
                        val `val` = intValues[pixel++]
                        byteBuffer.put(((`val` shr 16) and 0xFF).toByte())
                        byteBuffer.put(((`val` shr 8) and 0xFF).toByte())
                        byteBuffer.put((`val` and 0xFF).toByte())
                    }
                }
                inputFeature0.loadBuffer(byteBuffer)

                // Run inference
                val outputs = model.process(inputFeature0)
                val outputFeature0 = outputs.outputFeature0AsTensorBuffer

                println(outputFeature0.floatArray)
                val confidences = outputFeature0.floatArray
                println(confidences.indices)
                // find the index of the class with the biggest confidence.
                var maxPos = 0
                var maxConfidence = 0f
                for (i in confidences.indices) {
                    if (confidences[i] > maxConfidence) {
                        maxConfidence = confidences[i]
                        maxPos = i
                    }
                }
                val classes = arrayOf("Person", "Flower", "Chair")
                textView.text = classes[maxPos]
            }

        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0

                var surfaceTexture = textureView.surfaceTexture
                var surface = Surface(surfaceTexture)

                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface);

                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback() {
                    override fun onConfigured(p0: CameraCaptureSession) {
                        p0.setRepeatingRequest(captureRequest.build(), null, null);
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {
                    }

                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {
            }

            override fun onError(p0: CameraDevice, p1: Int) {
            }

        }, handler)
    }
}