package com.example.recepiesuggestor.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.imagedescription.ImageDescriber;
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions;
import com.google.mlkit.genai.imagedescription.ImageDescription;
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest;
import com.google.mlkit.genai.common.GenAiException;
import java.util.concurrent.ExecutionException;

public class ImageDescriberSingleton {
    // 1. The single instance of the class
    private static ImageDescriberSingleton instance;

    private static Context activityContext;

    private final ImageDescriber imageDescriber;

    // 3. Private constructor to prevent direct instantiation
    private ImageDescriberSingleton(Context context) {
        // Initialize the ImageLabeler here
        // Using a custom confidence threshold (e.g., 70%) for demonstration
        ImageDescriberOptions options =
                ImageDescriberOptions.builder(context).build();

        // Create the labeler instance
        imageDescriber = ImageDescription.getClient(options);
        activityContext = context;
    }

    // 4. Public method to get the single instance
    public static synchronized ImageDescriberSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new ImageDescriberSingleton(context);
        }
        return instance;
    }

    public void prepareAndStartImageDescription(
            Bitmap bitmap,
            ImageProxy imageProxy
    ) throws ExecutionException, InterruptedException {
        try {
            int featureStatus = imageDescriber.checkFeatureStatus().get();
            if (featureStatus == FeatureStatus.DOWNLOADABLE) {

                imageDescriber.downloadFeature(new DownloadCallback() {
                    @Override
                    public void onDownloadCompleted() {
                        startImageDescriptionRequest(bitmap, imageProxy);
                    }

                    @Override
                    public void onDownloadFailed(GenAiException e) { /*imageProxy.close();*/ }

                    @Override
                    public void onDownloadProgress(long totalBytesDownloaded) {}

                    @Override
                    public void onDownloadStarted(long bytesDownloaded) {}
                });
            } else if (featureStatus == FeatureStatus.DOWNLOADING) {
                // Inference request will automatically run once feature is
                // downloaded.
                // If Gemini Nano is already downloaded on the device, the
                // feature-specific LoRA adapter model will be downloaded
                // very quickly. However, if Gemini Nano is not already
                // downloaded, the download process may take longer.
                startImageDescriptionRequest(bitmap, imageProxy);
            } else if (featureStatus == FeatureStatus.AVAILABLE) {
                startImageDescriptionRequest(bitmap, imageProxy);
            } else if (featureStatus == FeatureStatus.UNAVAILABLE) {
                //imageProxy.close();
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.d("ingredients", e.toString());
            imageProxy.close();
        }
    }

    public void startImageDescriptionRequest(
            Bitmap bitmap,
            ImageProxy imageProxy
    ) {
        // Create task request
        ImageDescriptionRequest imageDescriptionRequest =
                ImageDescriptionRequest.builder(bitmap).build();

        // Start image description request with streaming response
        imageDescriber.runInference(imageDescriptionRequest, newText -> {
            Log.d("ingredients", newText);
            //imageProxy.close();
        });

    }

    // Optional: Close the labeler when the app shuts down (e.g., in onDestroy)
    public void close() {
        imageDescriber.close();
    }
}
