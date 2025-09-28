package com.example.recepiesuggestor.services;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WhisperApiService {
    private static final String WHISPERX_SERVER_URL = "http://localhost:5001/command"; // Use localhost with adb reverse port forwarding
    private static final String TAG = "WhisperXService";

    public interface TranscriptionCallback {
        void onSuccess(String transcription);
        void onError(String error);
    }

    private static WhisperApiService instance;
    private final ExecutorService executor;
    private final Context context;

    private WhisperApiService(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static synchronized WhisperApiService getInstance(Context context) {
        if (instance == null) {
            instance = new WhisperApiService(context);
        }
        return instance;
    }

    public void transcribeAudio(File audioFile, TranscriptionCallback callback) {
        executor.execute(() -> {
            try {
                String transcription = makeWhisperXApiCall(audioFile);
                callback.onSuccess(transcription);

            } catch (Exception e) {
                Log.e(TAG, "Failed to transcribe audio with WhisperX", e);
                callback.onError(e.getMessage());
            }
        });
    }

    private String makeWhisperXApiCall(File audioFile) throws Exception {
        URL url = new URL(WHISPERX_SERVER_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String boundary = "----formdata-boundary-" + System.currentTimeMillis();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000); // 5 second timeout
        conn.setReadTimeout(10000);   // 10 second timeout

        try (OutputStream os = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true)) {

            // Add file field
            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"").append("\r\n");
            writer.append("Content-Type: audio/wav").append("\r\n");
            writer.append("\r\n");
            writer.flush();

            // Copy file data
            try (FileInputStream fis = new FileInputStream(audioFile)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            os.flush();

            writer.append("\r\n");

            // End boundary
            writer.append("--" + boundary + "--").append("\r\n");
            writer.flush();
        }

        int responseCode = conn.getResponseCode();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode == 200 ? conn.getInputStream() : conn.getErrorStream(),
                "UTF-8"))) {

            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            if (responseCode != 200) {
                throw new Exception("WhisperX server call failed: " + response.toString());
            }

            return extractTranscriptionFromWhisperX(response.toString());
        }
    }

    private String extractTranscriptionFromWhisperX(String response) throws Exception {
        JSONObject jsonResponse = new JSONObject(response);

        if (!jsonResponse.getBoolean("success")) {
            throw new Exception("WhisperX transcription failed");
        }

        String text = jsonResponse.getString("text");
        boolean isUpdateCommand = jsonResponse.getBoolean("is_update_command");

        Log.d(TAG, "WhisperX result - Text: '" + text + "', Update command: " + isUpdateCommand);

        return text.trim();
    }
}