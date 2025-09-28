package com.example.recepiesuggestor.services;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpeechRecognitionService implements WhisperApiService.TranscriptionCallback {
    private static final String TAG = "SpeechRecognition";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public interface VoiceCommandListener {
        void onUpdateCommand();
    }

    private static SpeechRecognitionService instance;
    private final Context context;
    private final WhisperApiService whisperService;
    private VoiceCommandListener commandListener;

    private AudioRecord audioRecord;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private Thread recordingThread;

    private SpeechRecognitionService(Context context) {
        this.context = context.getApplicationContext();
        this.whisperService = WhisperApiService.getInstance(context);
    }

    public static synchronized SpeechRecognitionService getInstance(Context context) {
        if (instance == null) {
            instance = new SpeechRecognitionService(context);
        }
        return instance;
    }

    public void setVoiceCommandListener(VoiceCommandListener listener) {
        this.commandListener = listener;
    }

    public void startListening() {
        if (isRecording.get()) return;

        try {
            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }

            isRecording.set(true);
            audioRecord.startRecording();

            recordingThread = new Thread(this::recordAudio);
            recordingThread.start();

            Log.d(TAG, "Started listening for voice commands");

        } catch (SecurityException e) {
            Log.e(TAG, "Microphone permission not granted", e);
        }
    }

    public void stopListening() {
        isRecording.set(false);
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (recordingThread != null) {
            recordingThread.interrupt();
        }
        Log.d(TAG, "Stopped listening for voice commands");
    }

    private void recordAudio() {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        byte[] buffer = new byte[bufferSize];

        // Record for 3 seconds
        long recordingDuration = 3000;
        long startTime = System.currentTimeMillis();

        try {
            File audioFile = new File(context.getCacheDir(), "voice_command.wav");
            FileOutputStream fos = new FileOutputStream(audioFile);

            // Write WAV header
            writeWavHeader(fos, 0, SAMPLE_RATE, 1);

            int totalBytes = 0;
            while (isRecording.get() && (System.currentTimeMillis() - startTime) < recordingDuration) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
            }

            fos.close();

            // Update WAV header with actual data size
            updateWavHeader(audioFile, totalBytes);

            // Process the recorded audio
            if (totalBytes > 0) {
                whisperService.transcribeAudio(audioFile, this);
            }

        } catch (IOException e) {
            Log.e(TAG, "Error recording audio", e);
        }

        // Auto-restart listening after processing
        if (isRecording.get()) {
            startListening();
        }
    }

    private void writeWavHeader(FileOutputStream fos, int dataSize, int sampleRate, int channels) throws IOException {
        byte[] header = new byte[44];

        // RIFF header
        header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
        int fileSize = 36 + dataSize;
        header[4] = (byte) (fileSize & 0xff);
        header[5] = (byte) ((fileSize >> 8) & 0xff);
        header[6] = (byte) ((fileSize >> 16) & 0xff);
        header[7] = (byte) ((fileSize >> 24) & 0xff);

        // WAVE header
        header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

        // fmt chunk
        header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
        header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0; // fmt chunk size
        header[20] = 1; header[21] = 0; // audio format (PCM)
        header[22] = (byte) channels; header[23] = 0; // number of channels

        // Sample rate
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);

        int byteRate = sampleRate * channels * 2;
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);

        header[32] = (byte) (channels * 2); header[33] = 0; // block align
        header[34] = 16; header[35] = 0; // bits per sample

        // data chunk
        header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
        header[40] = (byte) (dataSize & 0xff);
        header[41] = (byte) ((dataSize >> 8) & 0xff);
        header[42] = (byte) ((dataSize >> 16) & 0xff);
        header[43] = (byte) ((dataSize >> 24) & 0xff);

        fos.write(header);
    }

    private void updateWavHeader(File file, int dataSize) throws IOException {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw")) {
            // Update file size
            raf.seek(4);
            int fileSize = 36 + dataSize;
            raf.write(fileSize & 0xff);
            raf.write((fileSize >> 8) & 0xff);
            raf.write((fileSize >> 16) & 0xff);
            raf.write((fileSize >> 24) & 0xff);

            // Update data size
            raf.seek(40);
            raf.write(dataSize & 0xff);
            raf.write((dataSize >> 8) & 0xff);
            raf.write((dataSize >> 16) & 0xff);
            raf.write((dataSize >> 24) & 0xff);
        }
    }

    @Override
    public void onSuccess(String transcription) {
        Log.d(TAG, "Transcription: " + transcription);

        // Check for "update" command (case insensitive)
        if (transcription.toLowerCase().contains("update")) {
            Log.d(TAG, "Update command detected!");
            if (commandListener != null) {
                commandListener.onUpdateCommand();
            }
        }
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Transcription error: " + error);
    }
}