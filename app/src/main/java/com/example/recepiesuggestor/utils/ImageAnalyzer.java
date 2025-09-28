package com.example.recepiesuggestor.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.recepiesuggestor.models.ImageDescriberSingleton;
import com.example.recepiesuggestor.models.ImageLabelerSingleton;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.camera.core.ExperimentalGetImage
public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    private final ImageDescriberSingleton imageDescriberSingleton;
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();

    public ImageAnalyzer(Context context) {
        imageDescriberSingleton = ImageDescriberSingleton.getInstance(context);
    }

    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {

            analysisExecutor.execute(() -> {
                try {
                    Bitmap bitmap = ImageUtils.bitmapFromImageProxy(imageProxy);
                    imageDescriberSingleton.prepareAndStartImageDescription(bitmap, imageProxy);

                } catch (ExecutionException e) {
                    imageProxy.close(); // Close proxy if setup fails synchronously
                } catch (InterruptedException e) {
                    // Handle thread interruption.
                    Log.e("ingredients", "Describer setup interrupted. Closing proxy.", e);
                    Thread.currentThread().interrupt();
                    imageProxy.close(); // Close proxy if setup fails synchronously
                } catch (Exception e) {
                    // Catch any other synchronous runtime exceptions during execution.
                    Log.e("ingredients", "Unexpected error in background analysis. Closing proxy.", e);
                    imageProxy.close();
                }
            });
        } else {
            imageProxy.close();
        }
    }
}
