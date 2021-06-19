package com.example.experiment;

import androidx.camera.core.ImageProxy;

import com.example.experiment.util.GraphicOverlay;
import com.google.mlkit.common.MlKitException;

public interface VisionImageProcessor {

    void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) throws MlKitException;
    void stop();
}
