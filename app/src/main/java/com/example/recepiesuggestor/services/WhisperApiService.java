package com.example.recepiesuggestor.services;

import android.content.Context;
import android.util.Log;
import com.example.recepiesuggestor.config.ApiKeyManager;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WhisperApiService {
    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";
    private static final String TAG = "WhisperApiService";

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
                String apiKey = ApiKeyManager.getInstance(context).getApiKey();
                if (apiKey == null) {
                    callback.onError("API key not configured");
                    return;
                }

                String transcription = makeWhisperApiCall(apiKey, audioFile);
                callback.onSuccess(transcription);

            } catch (Exception e) {
                Log.e(TAG, "Failed to transcribe audio", e);
                callback.onError(e.getMessage());
            }
        });
    }

    private String makeWhisperApiCall(String apiKey, File audioFile) throws Exception {
        URL url = new URL(WHISPER_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        String boundary = "----formdata-boundary-" + System.currentTimeMillis();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        conn.setDoOutput(true);

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

            // Add model field
            writer.append("--" + boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"model\"").append("\r\n");
            writer.append("\r\n");
            writer.append("whisper-1");
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
                throw new Exception("Whisper API call failed: " + response.toString());
            }

            return extractTranscriptionFromResponse(response.toString());
        }
    }

    private String extractTranscriptionFromResponse(String response) throws Exception {
        JSONObject jsonResponse = new JSONObject(response);
        return jsonResponse.getString("text").trim();
    }
}