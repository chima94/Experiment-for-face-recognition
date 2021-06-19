package com.example.experiment

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.experiment.util.GraphicOverlay
import com.google.mlkit.common.MlKitException
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private var preview : Preview? = null
    private lateinit var previewView: PreviewView
    private lateinit var graphicOverlay: GraphicOverlay
    private val lensFacing = CameraSelector.LENS_FACING_FRONT
    private var cameraSelector : CameraSelector? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var imageProcessor: VisionImageProcessor? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        previewView = findViewById(R.id.preview_view)
        graphicOverlay = findViewById(R.id.graphic_overlay)


        ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory.getInstance(application))
            .get(CameraViewModel::class.java)
            .processCameraProvider
            .observe(this, Observer { provider : ProcessCameraProvider? ->
               cameraProvider = provider
                if(allPermissionsGranted()){
                    bindCameraUseCases()
                }

            })


        if(!allPermissionsGranted()){
            runtimePermissions
        }

    }


    private fun bindCameraUseCases(){
        if(cameraProvider != null){
            cameraProvider!!.unbindAll()
            bindPreviewUseCases()
            bindAnalysisUseCase()
        }
    }


    private fun bindPreviewUseCases(){
        if(preview != null){
            cameraProvider!!.unbind(preview)
        }

        val builder = Preview.Builder()
        builder.setTargetResolution(Size(480, 360))
        preview = builder.build()
        preview!!.setSurfaceProvider(previewView.createSurfaceProvider())
        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, preview)
    }

    private fun bindAnalysisUseCase(){
        if(analysisUseCase != null){
            cameraProvider!!.unbind(analysisUseCase)
        }

        if(imageProcessor != null){
            imageProcessor!!.stop()
        }

        val faceDetectionOptions = FaceDetectorOptions.Builder()
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .build()

        imageProcessor = FaceDetectorProcessor(this, faceDetectionOptions)
        val builder = ImageAnalysis.Builder()
        analysisUseCase = builder.build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analysisUseCase?.setAnalyzer(
            ContextCompat.getMainExecutor(this),

            { imageProxy: ImageProxy ->
                if (needUpdateGraphicOverlayImageSourceInfo) {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay.setImageSourceInfo(imageProxy.width, imageProxy.height, true)
                    } else {
                        graphicOverlay.setImageSourceInfo(imageProxy.height, imageProxy.width, true)
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }
                try {
                    imageProcessor!!.processImageProxy(imageProxy, graphicOverlay)
                }catch (e : MlKitException){
                    Log.e("MAINACTIVITY", "failed to process image" + e.localizedMessage)
                }
            }
        )

        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, analysisUseCase)
    }











    private val requiredPermission : Array<String?>
        get() = try{
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if(ps != null && ps.isNotEmpty()){
                ps
            }else{
                arrayOfNulls(0)
            }
        }catch (e : Exception){
            arrayOfNulls(0)
        }


    private fun allPermissionsGranted() : Boolean {
        for (permission in requiredPermission) {
            if (!isPermissionGranted(this, permission)) {
                return false
            }
        }

        return true
    }



    private val runtimePermissions: Unit
        get() {
            val allNeededPermissions: MutableList<String?> = ArrayList()
            for (permission in requiredPermission) {
                if (!isPermissionGranted(this, permission)) {
                    allNeededPermissions.add(permission)
                }
            }
            if (allNeededPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    allNeededPermissions.toTypedArray(),
                    PERMISSION_REQUESTS
                )
            }
        }

    private fun isPermissionGranted(
        context: Context,
        permission: String?
    ): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission!!)
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("MAINACTIVITY", "Permission granted: $permission")
            return true
        }
        Log.i("MAINACTIVITY", "Permission NOT granted: $permission")
        return false
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i("MAINACTIVITY", "Permission granted!")
        if (allPermissionsGranted()) {
            Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show()
            bindCameraUseCases()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    companion object{
        private const val PERMISSION_REQUESTS = 1
    }

}

