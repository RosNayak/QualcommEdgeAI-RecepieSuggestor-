package com.example.recepiesuggestor.utils;

import android.Manifest;
import android.content.pm.PackageManager;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class PermissionHelper {

    private final ComponentActivity activity;
    private final ActivityResultLauncher<String> cameraPermissionLauncher;
    private final ActivityResultLauncher<String> microphonePermissionLauncher;

    private Runnable onCameraGranted;
    private Runnable onCameraDenied;
    private Runnable onMicGranted;
    private Runnable onMicDenied;

    public PermissionHelper(ComponentActivity activity) {
        this.activity = activity;

        cameraPermissionLauncher =
                activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        if (onCameraGranted != null) onCameraGranted.run();
                    } else {
                        if (onCameraDenied != null) onCameraDenied.run();
                    }
                });

        microphonePermissionLauncher =
                activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        if (onMicGranted != null) onMicGranted.run();
                    } else {
                        if (onMicDenied != null) onMicDenied.run();
                    }
                });
    }

    /** Checks CAMERA permission; if not granted, requests it. */
    public void ensureCameraPermission(Runnable onGranted, Runnable onDenied) {
        this.onCameraGranted = onGranted;
        this.onCameraDenied = onDenied;

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (this.onCameraGranted != null) this.onCameraGranted.run();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /** Checks RECORD_AUDIO permission; if not granted, requests it. */
    public void ensureMicrophonePermission(Runnable onGranted, Runnable onDenied) {
        this.onMicGranted = onGranted;
        this.onMicDenied = onDenied;

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            if (this.onMicGranted != null) this.onMicGranted.run();
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }
}
