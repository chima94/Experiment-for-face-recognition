package com.example.experiment

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.experiment.util.FaceGraphic
import com.example.experiment.util.GraphicOverlay
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.lang.Exception

class FaceDetectorProcessor(val context: Context, detectorOptions : FaceDetectorOptions?)
    :VisionProcessorBase<List<Face>>(context)

{

    private val detector : FaceDetector;

    init {
        val options = detectorOptions
            ?: FaceDetectorOptions.Builder()
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .enableTracking()
                .build()
        detector = FaceDetection.getClient(options)
    }

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun onSuccess(faces: List<Face>, graphicOverlay: GraphicOverlay) {
        for(face in faces){
            graphicOverlay.add(FaceGraphic(graphicOverlay, face))
        }
    }

    override fun onFailure(e: Exception) {
        Log.e("FaceDetectorProcessor", "Face detection failed $e")
    }


}