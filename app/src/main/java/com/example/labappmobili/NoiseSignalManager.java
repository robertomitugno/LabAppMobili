package com.example.labappmobili;

import static com.example.labappmobili.GridTileProvider.latitudineInMeters;
import static com.example.labappmobili.GridTileProvider.longitudineInMeters;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.room.Room;

import com.example.labappmobili.RoomDB.Noise.Noise;
import com.example.labappmobili.RoomDB.Noise.NoiseDB;
import com.example.labappmobili.RoomDB.Noise.NoiseDao;
import com.google.android.gms.maps.GoogleMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class NoiseSignalManager {

    private static final int PERMISSION_AUDIO_CODE = 2;
    private static Context context;
    Location currentLocation;
    private static GoogleMap googleMap;
    private static AudioRecord audioRecord;
    private static boolean isRecording = false;

    static GridTileProvider gridTileProvider;

    static NoiseDB noiseDB;

    public NoiseSignalManager(Context context) {
        this.context = context;
        initializeRoomDatabase();
    }
    public NoiseSignalManager(Context context, GoogleMap map) {
        this.context = context;
        this.googleMap = map;
        initializeRoomDatabase();
    }

    public NoiseSignalManager(Context context, Location currentLocation, GoogleMap map) {
        this.context = context;
        this.googleMap = map;
        this.currentLocation = currentLocation;
        initializeRoomDatabase();
    }

    private static void startRecording() {
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
            }
        }
    }

    private static void initializeRoomDatabase() {
        noiseDB = Room.databaseBuilder(context, NoiseDB.class, "NoiseDB").build();
    }

    static double getNoiseLevel(){
        startRecording();
        if(isRecording) {
            short[] audioData = new short[audioRecord.getBufferSizeInFrames()];
            audioRecord.read(audioData, 0, audioData.length);
            return calculateNoiseLevel(audioData);
        }
        return 0;
    }

    private static double calculateNoiseLevel(short[] audioData) {
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

    static void showNoiseMap(){
        gridTileProvider = new GridTileProvider(context, getAllNoiseValue());
        GridManager.getInstance().setGrid(googleMap, gridTileProvider);
    }

    static String insertNoiseMeasurement(Location currentLocation, long time) {

        initializeRoomDatabase(); // Inizializza il database Room

        Date date = new Date();

        double noiseLevel = getNoiseLevel();

        double latitudine = latitudineInMeters(currentLocation.getLatitude());
        double longitudine = longitudineInMeters(currentLocation.getLongitude());

        GridTileProvider gridTileProvider = new GridTileProvider(context, getAllNoiseValue());

        if (gridTileProvider.checkTimeArea(currentLocation, time).startsWith(context.getResources().getString(R.string.waiting))){
            return gridTileProvider.checkTimeArea(currentLocation, time);
        }

        Noise noiseMeasurement = new Noise(latitudine, longitudine, noiseLevel, date.getTime());

        Log.d("Misurazione","Inserimento rumore : " + latitudine + " : " + longitudine + " : " + noiseLevel);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            NoiseDao noiseDao = noiseDB.getNoiseDao();
            noiseDao.insertNoise(noiseMeasurement);

        });

        //getNoiseListInBackground();

        return context.getResources().getString(R.string.measurement_complete);
    }


    static List<Noise> getAllNoiseValue() {
        if (noiseDB == null) {
            return new ArrayList<>(); // Il database non è ancora inizializzato
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        AtomicReference<List<Noise>> noiseListRef = new AtomicReference<>(new ArrayList<>());

        executorService.execute(() -> {
            NoiseDao noiseDao = noiseDB.getNoiseDao();
            noiseListRef.set(noiseDao.getAllNoise());
        });

        executorService.shutdown();

        while (!executorService.isTerminated()) {
            // Attendi fino a quando il thread non è terminato
        }

        return noiseListRef.get();
    }


    static void deleteNoiseMeasurement(Object noise) {

        initializeRoomDatabase(); // Inizializza il database Room

        Log.d("Misurazione","Eliminazione noise : " + noise);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            // Esegui l'operazione di inserimento nel database Room su un thread separato
            NoiseDao noiseDao = noiseDB.getNoiseDao();
            noiseDao.deleteNoiseById(((Noise) noise).getId());
        });
    }
    public static int getNoiseColor(double noiseValue) {
        if (noiseValue > 50) {
            return Color.RED;
        } else if (noiseValue > 30) {
            return Color.YELLOW;
        } else if (noiseValue <= 30) {
            return Color.GREEN;
        } else {
            return Color.TRANSPARENT;
        }
    }

}
