//package com.example.visionguidify
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.content.Intent
//import android.graphics.Bitmap
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import android.graphics.RectF
//import android.graphics.SurfaceTexture
//import android.graphics.Typeface
//import android.hardware.camera2.CameraCaptureSession
//import android.hardware.camera2.CameraDevice
//import android.hardware.camera2.CameraManager
//import android.os.Bundle
//import android.os.Handler
//import android.os.HandlerThread
//import android.speech.tts.TextToSpeech
//import android.util.Log
//import android.view.Surface
//import android.view.TextureView
//import android.widget.Button
//import android.widget.ImageView
//import android.widget.TextView
//import androidx.appcompat.app.AppCompatActivity
//import com.example.visionguidify.ml.DetectQuant
//import org.tensorflow.lite.DataType
//import org.tensorflow.lite.support.common.FileUtil
//import org.tensorflow.lite.support.image.ImageProcessor
//import org.tensorflow.lite.support.image.ops.ResizeOp
//import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.util.Locale
//
//class CameraActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
//
//    var colors = listOf<Int>(
//        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
//        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
//    lateinit var imageView: ImageView
//    lateinit var labels:List<String>
//    lateinit var textureView: TextureView
//    lateinit var cameraManager: CameraManager
//    lateinit var handler: Handler
//    lateinit var cameraDevice: CameraDevice
//    lateinit var bitmap: Bitmap
//    lateinit var model: DetectQuant
//    lateinit var imageProcessor: ImageProcessor
//    lateinit var button: Button
//    private  var tts: TextToSpeech? = null
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_camera)
//
//        labels = FileUtil.loadLabels(this, "labels.txt")
//        model = DetectQuant.newInstance(this)
//        imageProcessor = ImageProcessor.Builder().add(ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR)).build()
//
//        val handlerThread = HandlerThread("videoThread")
//        handlerThread.start()
//        handler = Handler(handlerThread.looper);
//
//        textureView = findViewById(R.id.textureView)
//        imageView = findViewById(R.id.imageView)
//        button = findViewById(R.id.scanButton)
//        tts = TextToSpeech(this, this)
//
//
////        textView = findViewById(R.id.detectedClasses)
//        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
//            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
//                openCamera();
//            }
//
//            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
//            }
//
//            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
//                return false
//            }
//
//            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
//                bitmap = textureView.bitmap!!
//
//                val imageSize = 320
//                val byteBuffer = ByteBuffer.allocateDirect(1 * imageSize * imageSize *  3)
//                byteBuffer.order(ByteOrder.nativeOrder())
//
//                val intValues = IntArray(imageSize * imageSize)
//                bitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)
//
//                var pixel = 0
//                for (i in 0 until imageSize) {
//                    for (j in 0 until imageSize) {
//                        val valRGB = intValues[pixel++] // RGB
//                        byteBuffer.put((valRGB shr 16 and 0xFF).toByte())
//                        byteBuffer.put((valRGB shr 8 and 0xFF).toByte())
//                        byteBuffer.put((valRGB and 0xFF).toByte())
//                    }
//                }
//
//                val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 320, 320, 3), DataType.UINT8)
//                inputFeature0.loadBuffer(byteBuffer)
//
//                val outputs = model.process(inputFeature0)
//                val detection_scores = outputs.outputFeature0AsTensorBuffer.floatArray
//                val detection_boxes = outputs.outputFeature1AsTensorBuffer.floatArray
//                val num_detections = outputs.outputFeature2AsTensorBuffer.floatArray
//                val detection_classes  = outputs.outputFeature3AsTensorBuffer.floatArray
//
//                Log.d("Tag", "Output Tensor 0: ${outputs.outputFeature0AsTensorBuffer.floatArray.joinToString(", ")}")
//                Log.d("Tag", "Output Tensor 1: ${outputs.outputFeature1AsTensorBuffer.floatArray.joinToString(", ")}")
//                Log.d("Tag", "Output Tensor 2: ${outputs.outputFeature2AsTensorBuffer.floatArray.joinToString(", ")}")
//                Log.d("Tag", "Output Tensor 3: ${outputs.outputFeature3AsTensorBuffer.floatArray.joinToString(", ")}")
//
//
//                val focalLength = 20
//                val knownWidth = 50
//
//                val maxObjects = 5
//                val filteredIndices = detection_scores.withIndex()
//                    .filter { (_, score) -> score > 0.5 }
//                    .sortedByDescending { (_, score) -> score }
//                    .take(maxObjects)
//                    .map { (index, _) -> index }
//
//                // Draw bounding boxes on the detected objects and display additional information
//                val canvas = Canvas(bitmap)
//                val paint = Paint().apply {
//                    color = Color.RED
//                    style = Paint.Style.STROKE
//                    strokeWidth = 4f
//                    textSize = 30f
//                    textAlign = Paint.Align.LEFT
//                    typeface = Typeface.DEFAULT_BOLD
//                }
//
//                val h = bitmap.height.toFloat()
//                val w = bitmap.width.toFloat()
//                paint.textSize = h/40f
//                paint.strokeWidth = h/150f
//                filteredIndices.forEach { index ->
//                    val x = index * 4
//                    paint.setColor(colors[index])
//                    paint.style = Paint.Style.STROKE
//                    canvas.drawRect(RectF(detection_boxes[x + 1] * w, detection_boxes[x] * h, detection_boxes[x + 3] * w, detection_boxes[x + 2] * h), paint)
//                    paint.style = Paint.Style.FILL
//                    val distance = (knownWidth * focalLength) / (detection_boxes[x + 3] - detection_boxes[x + 1])
//                    canvas.drawText(labels[detection_classes[index].toInt()] + " " + detection_scores[index], detection_boxes[x + 1] * w, detection_boxes[x] * h, paint)
//                    canvas.drawText("Distance: " + "%.2f".format(distance / 10) + " cm", detection_boxes[x + 1] * w, detection_boxes[x] * h - 25, paint)
//                    isBarcodeDetected(labels[detection_classes[index].toInt()])
//                }
//
//                imageView.setImageBitmap(bitmap)
//
//            }
//
//        }
//
//        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
//    }
//
//    override fun onInit(status: Int) {
//        if (status == TextToSpeech.SUCCESS) {
//            val result = tts?.setLanguage(Locale.US)
//            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                Log.e("TTS", "The Language not supported!")
//            }
//        } else {
//            Log.e("TTS", "Initialization failed")
//        }
//    }
//
//    private fun speakText(text: String) {
//        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
//    }
//
//    private fun isBarcodeDetected(detected: String) {
//        if (detected == "qr_code") {
//            speakText("QR detected")
//            val intent = Intent(this, ScanningActivity::class.java)
//            startActivity(intent)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        model.close()
//        tts?.stop()
//        tts?.shutdown()
//    }
//
//    @SuppressLint("MissingPermission")
//    private fun openCamera(){
//        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback(){
//            override fun onOpened(p0: CameraDevice) {
//                cameraDevice = p0
//
//                var surfaceTexture = textureView.surfaceTexture
//                var surface = Surface(surfaceTexture)
//
//                var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//                captureRequest.addTarget(surface);
//
//                cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback() {
//                    override fun onConfigured(p0: CameraCaptureSession) {
//                        p0.setRepeatingRequest(captureRequest.build(), null, null);
//                    }
//
//                    override fun onConfigureFailed(p0: CameraCaptureSession) {
//                    }
//
//                }, handler)
//            }
//
//            override fun onDisconnected(p0: CameraDevice) {
//            }
//
//            override fun onError(p0: CameraDevice, p1: Int) {
//            }
//
//        }, handler)
//    }
//}