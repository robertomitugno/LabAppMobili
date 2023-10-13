package com.example.labappmobili;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.IOException;
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
    private static final int WIFI_UPDATE_INTERVAL = 5000;


    private TextView noiseLevelText;
    private AudioRecord audioRecord;
    private boolean isRecording = false;


    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        setContentView(R.layout.activity_main);

        mapSearchView = findViewById(R.id.mapSearch);

        wifiUpdateHandler = new Handler();
        wifiUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateWifiSignalStrength();
                updateNoiseLevel();
                wifiUpdateHandler.postDelayed(this, WIFI_UPDATE_INTERVAL);
            }
        }, WIFI_UPDATE_INTERVAL);


        noiseLevelText = findViewById(R.id.noiseValue);
        initAudioRecorder();

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

        // Crea una TileOverlayOptions per la tua griglia colorata
        //TileOverlayOptions tileOverlayOptions = new TileOverlayOptions();

        // Aggiungi la TileOverlay alla mappa
        //TileOverlay tileOverlay = myMap.addTileOverlay(tileOverlayOptions);

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

        if (requestCode == AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // L'utente ha concesso l'autorizzazione, inizia la registrazione audio
                initAudioRecorder();
            } else {
                // L'utente ha negato l'autorizzazione, gestisci di conseguenza
                Toast.makeText(this, "Audio recording permission denied, please allow the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void updateWifiSignalStrength() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        wifiSignalStrengthText = findViewById(R.id.wifiValue);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int signalStrength = wifiInfo.getRssi() + 127;
            wifiSignalStrengthText.setText("WiFi Signal Strength: " + signalStrength + " dBm");
        } else {
            wifiSignalStrengthText.setText("WiFi Signal Strength: N/A");
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