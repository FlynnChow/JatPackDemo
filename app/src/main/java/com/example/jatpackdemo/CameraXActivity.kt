package com.example.jatpackdemo

import android.content.pm.PackageManager
import android.graphics.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_camera_x.*
import java.io.File
import java.util.jar.Manifest

private const val REQUEST_CODE_PREMISSIONS = 10
private val REQUIRED_PREMISSIONS = arrayOf(android.Manifest.permission.CAMERA)

class CameraXActivity : AppCompatActivity() {
    private lateinit var viewFinder:TextureView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_x)
        initActivity()
    }

    private fun initActivity(){
        viewFinder = view_finder
        if(allPermissionsGranted())
            viewFinder.post{startCamera()}
        else
            ActivityCompat.requestPermissions(this, REQUIRED_PREMISSIONS, REQUEST_CODE_PREMISSIONS)
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == REQUEST_CODE_PREMISSIONS){
            if(allPermissionsGranted()){
                viewFinder.post{
                    startCamera()
                }
            }else{
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PREMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext,it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera(){
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(1,1))//设置宽高比
            setTargetResolution(Size(640,640))//设置固定分辨率
            setLensFacing(androidx.camera.core.CameraX.LensFacing.BACK)//设置相机ID
        }.build()
        val preview = Preview(previewConfig)
        preview.setOnPreviewOutputUpdateListener {
            //view更新变化监听，必须先删除再添加
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder)
            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }
        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setTargetAspectRatio(Rational(1,1))
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)//基于宽高比自定义分辨率
        }.build()
        val imageCapture = ImageCapture(imageCaptureConfig)
        on_camera.setOnClickListener {
            val file = File(externalMediaDirs.first(),"${System.currentTimeMillis()}.jpg")
            imageCapture.takePicture(file,object :ImageCapture.OnImageSavedListener{
                override fun onImageSaved(file: File) {
                    Toast.makeText(this@CameraXActivity,file.absolutePath,Toast.LENGTH_SHORT).show()
                }

                override fun onError(useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?) {
                    cause?.printStackTrace()
                }
            })
        }

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            //使用工作线程，防止故障
            val analyzerThread = HandlerThread("analyzerThread").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()
        val analyzerUserCase = ImageAnalysis(analyzerConfig).apply {
            analyzer = LuminosityAnalyzer()
        }

        CameraX.bindToLifecycle(this,preview,imageCapture,analyzerUserCase)
    }

    private fun updateTransform(){
        val matrix = Matrix()

        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f
        val rotationDegrees = when(viewFinder.display.rotation){
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotationDegrees.toFloat(),centerX,centerY)
        viewFinder.setTransform(matrix)
    }
}
