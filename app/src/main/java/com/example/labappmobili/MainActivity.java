package com.example.labappmobili;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final int FINE_PERMISSION_CODE = 1;
    private static final int AUDIO_PERMISSION_CODE = 2;
    private TextView wifiSignalStrengthText;
    private GoogleMap myMap;
    private SearchView mapSearchView;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;

    private Handler wifiUpdateHandler;
    private static final int WIFI_UPDATE_INTERVAL = 1000;

    private TextView noiseLevelText;

    private TextView lteSignal;
    private static final int NOISE_UPDATE_INTERVAL = 1000;

    private static final int LTE_UPDATE_INTERVAL = 1000;
    private AudioRecord audioRecord;
    private boolean isRecording = false;

    private WifiSignalManager wifiSignalManager;

    private TelephonyManager telephonyManager;

    private Handler lteUpdateHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapSearchView = findViewById(R.id.mapSearch);
        wifiSignalStrengthText = findViewById(R.id.wifiValue);
        noiseLevelText = findViewById(R.id.noiseValue);
        lteSignal = findViewById(R.id.lteValue);

        wifiSignalManager = new WifiSignalManager(this);
        wifiUpdateHandler = new Handler();
        wifiUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                wifiSignalManager.updateWifiSignalStrength();
                updateNoiseLevel();
                updateLTELevel();
                wifiUpdateHandler.postDelayed(this, WIFI_UPDATE_INTERVAL);
            }
        }, WIFI_UPDATE_INTERVAL);

        initAudioRecorder();

        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        lteUpdateHandler = new Handler();
        lteUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateLTELevel(); // Chiamata alla funzione per aggiornare il segnale LTE
                lteUpdateHandler.postDelayed(this, LTE_UPDATE_INTERVAL);
            }
        }, LTE_UPDATE_INTERVAL);

        //cerca un nuovo punto sulla mappa
        mapSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                String location = mapSearchView.getQuery().toString();
                List<Address> addressList = null;

                if (location != null) {
                    Geocoder geocoder = new Geocoder(MainActivity.this);
                    try {
                        addressList = geocoder.getFromLocationName(location, 1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Address address = addressList.get(0);
                    LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                    myMap.addMarker(new MarkerOptions().position(latLng).title(location));
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));

                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        getLasLocation();

    }

    private void getLasLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
            }, FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    currentLocation = location;

                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                    mapFragment.getMapAsync(MainActivity.this);
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        LatLng myLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        myMap.moveCamera(CameraUpdateFactory.newLatLng(myLocation));
        myMap.addMarker(new MarkerOptions().position(myLocation).title("My location"));

        myMap.getUiSettings().setZoomControlsEnabled(true);
        myMap.getUiSettings().setCompassEnabled(true);


        // Stampa le coordinate della tua posizione con Log.d
        Log.d("MainActivity", "My Location - Latitude: " + myLocation.latitude + ", Longitude: " + myLocation.longitude);

        // Passa le coordinate reali della mappa al GridTileProvider
        GridTileProvider gridTileProvider = new GridTileProvider();
        myMap.addTileOverlay(new TileOverlayOptions().tileProvider(gridTileProvider));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map, menu);
        return true;
    }

    //menu selezione tipi di mappa
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.mapNone) {
            myMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        }
        if (id == R.id.mapNormal) {
            myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        if (id == R.id.mapSatellite) {
            myMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if (id == R.id.mapHybrid) {
            myMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
        if (id == R.id.mapTerrian) {
            myMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLasLocation();
            } else {
                Toast.makeText(this, "Location permission denied, please allow the permission", Toast.LENGTH_SHORT).show();
            }
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        wifiUpdateHandler.removeCallbacksAndMessages(null);
    }


    private void initAudioRecorder() {
        int audioSource = MediaRecorder.AudioSource.MIC;
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // Richiedi l'autorizzazione per la registrazione audio
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION_CODE);
        } else {
            // Autorizzazione già concessa, inizia la registrazione audio
            audioRecord = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat, bufferSize);
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.startRecording();
                isRecording = true;
            } else {
                noiseLevelText.setText("Noise Level: N/A");
            }
        }
    }

    private void updateNoiseLevel() {
        if (isRecording) {
            short[] audioData = new short[audioRecord.getBufferSizeInFrames()];
            audioRecord.read(audioData, 0, audioData.length);
            double noiseLevel = calculateNoiseLevel(audioData);
            noiseLevelText.setText("Noise Level: " + noiseLevel + " dB");
        } else {
            noiseLevelText.setText("Noise Level: N/A");
        }
    }

    private void updateLTELevel() {
        if (isRecording) {
            int lteLevel = getLTELevel();
            lteSignal.setText("LTE Level: " + lteLevel + " dB");
        } else {
            lteSignal.setText("LTE Level: N/A");
        }
    }

    private int getLTELevel() {
        try {
            // Ottieni la forza del segnale LTE
            int lteSignalStrength = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                lteSignalStrength = telephonyManager.getSignalStrength().getLevel();
            }
            Log.i("LTE_TAG", "Signal Strength = " + lteSignalStrength);
            return lteSignalStrength;
        } catch (Exception e) {
            Log.e("LTE_TAG", "Exception: " + e.toString());
            return 0; // Ritorna un valore di default se si verifica un'eccezione
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