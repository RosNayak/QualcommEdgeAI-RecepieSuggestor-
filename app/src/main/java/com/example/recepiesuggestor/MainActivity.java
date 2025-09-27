package com.example.recepiesuggestor;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import com.example.recepiesuggestor.ui.CameraXController;
import com.example.recepiesuggestor.utils.PermissionHelper;

public class MainActivity extends AppCompatActivity {

    private CameraXController cameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreviewView previewView = findViewById(R.id.camera_preview);
        ImageButton switchBtn = findViewById(R.id.btn_switch_camera);

        cameraController = new CameraXController(this, previewView);
        PermissionHelper permissionHelper = new PermissionHelper(this);

        // Ask for camera permission, then start CameraX
        permissionHelper.ensureCameraPermission(
                () -> cameraController.start(),
                () -> Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        );

        if (switchBtn != null) {
            switchBtn.setOnClickListener(v -> cameraController.switchCamera());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Optional: free resources when backgrounded
        // cameraController.stop();
    }
}
