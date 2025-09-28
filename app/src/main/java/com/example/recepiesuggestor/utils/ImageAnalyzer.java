package com.example.recepiesuggestor.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.example.recepiesuggestor.models.ImageDescriberSingleton;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.camera.core.ExperimentalGetImage
public class ImageAnalyzer implements ImageAnalysis.Analyzer {

    private final long SKIP_FRAMES = 4; // To process every 4th frame (0, 4, 8, ...)
    private long frameCount = 0; // Starts at 0

    private final ImageDescriberSingleton imageDescriberSingleton;
    private final ExecutorService analysisExecutor = Executors.newSingleThreadExecutor();

    public ImageAnalyzer(Context context) {
        imageDescriberSingleton = ImageDescriberSingleton.getInstance(context);
    }

    public void analyze(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();

        if (mediaImage != null && frameCount % SKIP_FRAMES != 0) {

            analysisExecutor.execute(() -> {
                try {
                    Bitmap bitmap = ImageUtils.bitmapFromImageProxy(imageProxy);
                    imageProxy.close();
                    imageDescriberSingleton.prepareAndStartImageDescription(bitmap, imageProxy);
                } catch (ExecutionException e) {
                    // Handle exceptions thrown by the synchronous part of the Describer setup.
//                    Log.e("ingredients", "Describer setup failed due to ExecutionException. Closing proxy.", e);
                    imageProxy.close(); // Close proxy if setup fails synchronously
                } catch (InterruptedException e) {
                    // Handle thread interruption.
//                    Log.e("ingredients", "Describer setup interrupted. Closing proxy.", e);
                    Thread.currentThread().interrupt();
                    imageProxy.close(); // Close proxy if setup fails synchronously
                } catch (Exception e) {
                    // Catch any other synchronous runtime exceptions during execution.
//                    Log.e("ingredients", "Unexpected error in background analysis. Closing proxy.", e);
                    imageProxy.close();
                }
            });
        } else {
            imageProxy.close();
        }

        frameCount += 1;
    }
}
