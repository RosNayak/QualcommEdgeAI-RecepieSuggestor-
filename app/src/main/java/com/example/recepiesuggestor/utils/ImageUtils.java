package com.example.recepiesuggestor.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import androidx.camera.core.ImageProxy;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ImageUtils {

    /**
     * Converts an ImageProxy (typically in YUV_420_888 format) to a Bitmap.
     *
     * @param imageProxy The ImageProxy to convert.
     * @return A Bitmap representation of the image, or null if conversion fails.
     */
    public static Bitmap bitmapFromImageProxy(ImageProxy imageProxy) {
        if (imageProxy.getFormat() != ImageFormat.YUV_420_888) {
            // Fallback for non-YUV_420_888 formats if necessary, though CameraX often uses this.
            // For simplicity, this example primarily handles YUV_420_888.
            // You might need to handle other formats like JPEG directly if they occur.
            ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

        ByteBuffer yBuffer = imageProxy.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = imageProxy.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = imageProxy.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped NV21 and YUV_420_888 format
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 100, out);
        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        return bitmap;
    }
}
