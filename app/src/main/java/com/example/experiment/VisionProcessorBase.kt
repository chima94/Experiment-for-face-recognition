package com.example.experiment

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.ImageProxy
import com.example.experiment.util.GraphicOverlay
import com.example.experiment.util.ScopedExecutor
import com.example.experiment.util.addOnSuccessListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskExecutors
import com.google.mlkit.vision.common.InputImage

abstract class VisionProcessorBase<T>(context: Context) : VisionImageProcessor {

    private val executor = ScopedExecutor(TaskExecutors.MAIN_THREAD)
    private var isShutdown = false


    @SuppressLint("UnsafeExperimentalUsageError")
    override fun processImageProxy(image: ImageProxy?, graphicOverlay: GraphicOverlay?) {
       if(isShutdown){
           return
       }
        requestDetectInImage(
            InputImage.fromMediaImage(image?.image!!, image.imageInfo.rotationDegrees),
            graphicOverlay!!
        )
            .addOnCompleteListener { image.close() }
    }



    override fun stop() {
        executor.shutdown()
        isShutdown = true
    }


    private fun requestDetectInImage(
        image : InputImage,
        graphicOverlay: GraphicOverlay
    ): Task<T>{
        return setUpListener(detectInImage(image), graphicOverlay)
    }


    private fun setUpListener(
        task : Task<T>,
        graphicOverlay: GraphicOverlay
    ) : Task<T>{
        return task.addOnSuccessListener(
            executor,
            OnSuccessListener {results : T ->
                graphicOverlay.clear()
                this.onSuccess(results, graphicOverlay)
                graphicOverlay.postInvalidate()

            }
        )
            .addOnFailureListener(
                executor,
                OnFailureListener { e: Exception ->
                    graphicOverlay.clear()
                    graphicOverlay.postInvalidate()
                    this.onFailure(e)
                }
            )
    }



    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected abstract fun onSuccess(results: T, graphicOverlay: GraphicOverlay)

    protected abstract fun onFailure(e: Exception)
}