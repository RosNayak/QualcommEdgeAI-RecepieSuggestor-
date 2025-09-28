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

    private Runnable onGranted;
    private Runnable onDenied;

    public PermissionHelper(ComponentActivity activity) {
        this.activity = activity;

        cameraPermissionLauncher =
                activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        if (onGranted != null) onGranted.run();
                    } else {
                        if (onDenied != null) onDenied.run();
                    }
                });

        microphonePermissionLauncher =
                activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) {
                        if (onGranted != null) onGranted.run();
                    } else {
                        if (onDenied != null) onDenied.run();
                    }
                });
    }

    /** Checks CAMERA permission; if not granted, requests it. */
    public void ensureCameraPermission(Runnable onGranted, Runnable onDenied) {
        this.onGranted = onGranted;
        this.onDenied = onDenied;

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (this.onGranted != null) this.onGranted.run();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    /** Checks RECORD_AUDIO permission; if not granted, requests it. */
    public void ensureMicrophonePermission(Runnable onGranted, Runnable onDenied) {
        this.onGranted = onGranted;
        this.onDenied = onDenied;

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            if (this.onGranted != null) this.onGranted.run();
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }
}
