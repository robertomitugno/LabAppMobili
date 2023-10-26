package com.example.labappmobili;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.view.View;
import android.widget.TextView;
import androidx.core.app.ActivityCompat;

public class NoiseSignalManager {

    private static final int PERMISSION_AUDIO_CODE = 2;
    private final Context context;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private TextView noiseLevelText;

    public NoiseSignalManager(Context context) {
        this.context = context;
    }

    private void startRecording() {
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Richiedi l'autorizzazione per la registrazione audio
            ActivityCompat.requestPermissions((Activity) context, new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_AUDIO_CODE);
        } else {
            // Autorizzazione già concessa, inizia la registrazione audio
            int audioSource = MediaRecorder.AudioSource.MIC;
            int sampleRate = 44100;
            int channelConfig = AudioFormat.CHANNEL_IN_MONO;
            int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
            int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

            audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                isRecording = true;
            } else {
                noiseLevelText.setText(R.string.rumore_level_n_a);
            }
        }
    }

    public void updateNoiseLevel() {
        noiseLevelText = ((MainActivity) context).findViewById(R.id.noiseValue);

        startRecording();
        if (isRecording) {
            short[] audioData = new short[audioRecord.getBufferSizeInFrames()];
            audioRecord.read(audioData, 0, audioData.length);
            double noiseLevel = calculateNoiseLevel(audioData);
            noiseLevelText.setText("Livello di Rumore: " + noiseLevel + " dB");
        } else {
            noiseLevelText.setText("Livello di Rumore: N/D");
        }
    }

    private double calculateNoiseLevel(short[] audioData) {
        double sum = 0;
        for (short sample : audioData) {
            sum += sample * sample;
        }
        double rms = Math.sqrt(sum / audioData.length);
        // Imposta la soglia uditiva come valore di riferimento (0 dB)
        double threshold = 1.0; // Implementa una funzione per calcolare la soglia uditiva
        // Calcola il livello di rumore in dB rispetto alla soglia uditiva
        double db = 20 * Math.log10(rms / threshold);
        // Assicurati che il valore non sia negativo
        if (db < 0) {
            db = 0;
        }
        // Arrotonda il valore a due cifre dopo la virgola
        db = Math.round(db * 100.0) / 100.0;

        return db;
    }
}
