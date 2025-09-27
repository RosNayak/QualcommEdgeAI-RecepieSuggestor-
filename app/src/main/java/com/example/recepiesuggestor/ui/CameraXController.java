package com.example.recepiesuggestor.ui;

import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.annotation.MainThread;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class CameraXController {

    private final ComponentActivity activity;
    private final PreviewView previewView;

    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private Camera boundCamera;

    public CameraXController(ComponentActivity activity, PreviewView previewView) {
        this.activity = activity;
        this.previewView = previewView;
    }

    /** Initialize CameraX and bind the preview use case. Call this AFTER permission is granted. */
    @MainThread
    public void start() {
        ListenableFuture<ProcessCameraProvider> providerFuture =
                ProcessCameraProvider.getInstance(activity);

        providerFuture.addListener(() -> {
            try {
                cameraProvider = providerFuture.get();
                bindPreviewUseCase();
            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraXController", "Failed to get camera provider", e);
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    /** Rebind preview (used on first start and when switching cameras). */
    @MainThread
    public void bindPreviewUseCase() {
        if (cameraProvider == null) return;

        cameraProvider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            boundCamera = cameraProvider.bindToLifecycle(
                    activity, // LifecycleOwner (ComponentActivity implements it)
                    cameraSelector,
                    preview
            );
        } catch (IllegalStateException e) {
            Log.e("CameraXController", "bindToLifecycle failed", e);
        }
    }

    /** Toggle between back and front cameras. */
    @MainThread
    public void switchCamera() {
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        bindPreviewUseCase();
    }

    /** Optional: unbind all on stop if you want explicit control. */
    @MainThread
    public void stop() {
        if (cameraProvider != null) cameraProvider.unbindAll();
    }

    public Camera getBoundCamera() {
        return boundCamera;
    }
}
