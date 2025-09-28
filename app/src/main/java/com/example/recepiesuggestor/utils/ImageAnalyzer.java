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

    private final ImageLabelerSingleton imageLabelerSingleton;
    private final ImageDescriberSingleton imageDescriberSingleton;
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();

    public ImageAnalyzer(Context context) {
        imageLabelerSingleton = ImageLabelerSingleton.getInstance(context);
        imageDescriberSingleton = ImageDescriberSingleton.getInstance(context);
    }

    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null) {
            InputImage image =
                    InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
//            Log.d("ingredients", image.toString());
            Bitmap bitmap = ImageUtils.bitmapFromImageProxy(imageProxy);
//            imageLabelerSingleton.processImage(image, imageProxy);
//            try {
//                imageDescriberSingleton.prepareAndStartImageDescription(bitmap, imageProxy);
//            } catch (ExecutionException e) {
//                throw new RuntimeException(e);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }

            analysisExecutor.execute(() -> {
                try {
                    // This is the call that contains the potentially blocking checkFeatureStatus().get().
                    // It runs on the separate analysisExecutor thread, preventing the camera preview freeze.
                    imageDescriberSingleton.prepareAndStartImageDescription(bitmap, imageProxy);

                    // IMPORTANT: The ImageDescriberSingleton is now responsible for calling
                    // imageProxy.close() inside its success and failure callbacks.

                } catch (ExecutionException e) {
                    // Handle exceptions thrown by the synchronous part of the Describer setup.
                    Log.e("ingredients", "Describer setup failed due to ExecutionException. Closing proxy.", e);
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
