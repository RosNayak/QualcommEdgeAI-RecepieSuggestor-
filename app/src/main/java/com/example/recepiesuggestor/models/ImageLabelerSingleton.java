package com.example.recepiesuggestor.models;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.example.recepiesuggestor.data.IngredientAccumulator;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.List;

public class ImageLabelerSingleton {

    // 1. The single instance of the class
    private static ImageLabelerSingleton instance;
    private static Context activityContext;

    // 2. The ML Kit ImageLabeler object
    private final ImageLabeler labeler;

    // 3. Private constructor to prevent direct instantiation
    private ImageLabelerSingleton(Context context) {
        // Initialize the ImageLabeler here
        // Using a custom confidence threshold (e.g., 70%) for demonstration
        ImageLabelerOptions options =
                new ImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.7f)
                        .build();

        // Create the labeler instance
        labeler = ImageLabeling.getClient(options);
        activityContext = context;
    }

    // 4. Public method to get the single instance
    public static synchronized ImageLabelerSingleton getInstance(Context context) {
        if (instance == null) {
            instance = new ImageLabelerSingleton(context);
        }
        return instance;
    }

    // 5. Method to perform the actual labeling process
    public void processImage(InputImage image, ImageProxy imageProxy) {

        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        IngredientAccumulator ingredientAccumulator = IngredientAccumulator.getInstance();
                        ingredientAccumulator.addLabels(activityContext, labels);
                        imageProxy.close();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(activityContext, e.toString(), Toast.LENGTH_SHORT).show();
                        Log.d("ingredients", e.toString());
                    }
                });
    }

    // Optional: Close the labeler when the app shuts down (e.g., in onDestroy)
    public void close() {
        labeler.close();
    }

}
