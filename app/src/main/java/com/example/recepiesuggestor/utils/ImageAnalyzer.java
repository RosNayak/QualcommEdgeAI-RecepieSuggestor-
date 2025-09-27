package com.example.recepiesuggestor.utils;

import android.content.Context;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.recepiesuggestor.models.ImageLabelerSingleton;
import com.google.mlkit.vision.common.InputImage;

@androidx.camera.core.ExperimentalGetImage
public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    private final ImageLabelerSingleton imageLabelerSingleton;

    public ImageAnalyzer(Context context) {
        imageLabelerSingleton = ImageLabelerSingleton.getInstance(context);
    }

    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
            Log.d("ingredients", image.toString());
            imageLabelerSingleton.processImage(image, imageProxy);
        } else {
            imageProxy.close();
        }
    }
}
