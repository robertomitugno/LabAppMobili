package com.example.labappmobili;


import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import com.google.android.gms.tasks.Task;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final int PERMISSION_LOCATION_CODE = 1;
    public static final String FINE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION;

    public static final int PERMISSION_AUDIO_CODE = 2;
    public static final String RECORD_AUDIO_PERMISSION = android.Manifest.permission.RECORD_AUDIO;

    public static final int PERMISSION_NOTIFICATION_CODE = 731;
    public static final String NOTIFICATION_PERMISSION = android.Manifest.permission.POST_NOTIFICATIONS;

    public static final int PERMISSION_BACKGROUND_CODE = 6;
    public static final String BACKGROUND_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION;

    private WifiSignalManager wifiSignalManager;
    private NoiseSignalManager noiseSignalManager;
    private SearchView mapSearchView;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleMap myMap;
    LatLng currentLatLng;

    private TextView variableText;

    CheckBox lteCheckBox, wifiCheckBox, rumoreCheckBox;
    static boolean isWifiEnabled = true;
    static boolean isLteEnabled = true;

    Button startMeasure;

    // Dichiarazione del tuo handler
    private final Handler handler = new Handler();

    // Dichiarazione della tua variabile per l'intervallo di tempo
    private String measurementInterval = "5s";  // Default a 5 secondi

    float currentZoom;

    // Dichiarazione di lteSignalManager
    private LteSignalManager lteSignalManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mapSearchView = findViewById(R.id.mapSearch);


        variableText = findViewById(R.id.variableText);


        /*** SearchBox ***/
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


        // Collega le variabili ai CheckBox nell'XML
        lteCheckBox = findViewById(R.id.lteCheckBox);
        wifiCheckBox = findViewById(R.id.wifiCheckBox);
        rumoreCheckBox = findViewById(R.id.rumoreCheckBox);


        // Aggiungi un listener per gestire gli eventi di selezione
        CheckBox.OnCheckedChangeListener checkBoxListener = (buttonView, isChecked) -> {

            if (isChecked) {
                    // Aggiungi le azioni da eseguire quando un CheckBox viene selezionato
                int checkBoxId = buttonView.getId();

                if (checkBoxId == R.id.lteCheckBox) {
                    wifiCheckBox.setChecked(false);
                    rumoreCheckBox.setChecked(false);
                    // Azioni per la selezione di LTE
                    if (isLteEnabled) {
                        handler.removeCallbacks(updateWifiText);
                        handler.removeCallbacks(updateNoiseText);

                        findViewById(R.id.ltelegend).setVisibility(View.VISIBLE);
                        findViewById(R.id.noiselegend).setVisibility(View.INVISIBLE);
                        findViewById(R.id.wifilegend).setVisibility(View.INVISIBLE);

                        LteSignalManager lteSignalManager = new LteSignalManager(MainActivity.this, myMap);
                        lteSignalManager.showLteMap();

                        handler.post(updateLteText);

                    } else {
                        Toast.makeText(this, "Permesso non concesso. Modificare le impostazioni.", Toast.LENGTH_LONG).show();
                        buttonView.setChecked(false);
                    }
                }

                else if (checkBoxId == R.id.wifiCheckBox) {
                    lteCheckBox.setChecked(false);
                    rumoreCheckBox.setChecked(false);
                    // Azioni per la selezione di WiFi
                    if (isWifiEnabled) {
                        handler.removeCallbacks(updateLteText);
                        handler.removeCallbacks(updateNoiseText);

                        findViewById(R.id.ltelegend).setVisibility(View.INVISIBLE);
                        findViewById(R.id.noiselegend).setVisibility(View.INVISIBLE);
                        findViewById(R.id.wifilegend).setVisibility(View.VISIBLE);

                        wifiSignalManager = new WifiSignalManager(MainActivity.this, myMap);
                        WifiSignalManager.showWifiMap();

                        handler.post(updateWifiText);
                    } else {
                        Toast.makeText(this, "Permesso non concesso. Modificare le impostazioni.", Toast.LENGTH_LONG).show();
                        buttonView.setChecked(false);
                    }
                }

                else if (checkBoxId == R.id.rumoreCheckBox) {
                    lteCheckBox.setChecked(false);
                    wifiCheckBox.setChecked(false);
                    // Azioni per la selezione di Rumore
                    requestRuntimePermissionAudio();
                }
            } else {
                GridManager.getInstance().removeGrid();

                findViewById(R.id.wifilegend).setVisibility(View.INVISIBLE);
                findViewById(R.id.ltelegend).setVisibility(View.INVISIBLE);
                findViewById(R.id.noiselegend).setVisibility(View.INVISIBLE);

                handler.removeCallbacks(updateLteText);
                handler.removeCallbacks(updateWifiText);
                handler.removeCallbacks(updateNoiseText);


                variableText.setText("VISUALIZZA MISURAZIONI...");  // Pulisci il testo
                }

        };

        // Aggiungi il listener ai CheckBox
        lteCheckBox.setOnCheckedChangeListener(checkBoxListener);
        wifiCheckBox.setOnCheckedChangeListener(checkBoxListener);
        rumoreCheckBox.setOnCheckedChangeListener(checkBoxListener);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        /*** Inizializza la mappa ***/
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        /*** Richiedi l'autorizzazione per la posizione ***/
        //findViewById(R.id.my_location).setOnClickListener(v -> requestRuntimePermissionNotification());
        findViewById(R.id.my_location).setOnClickListener(v -> requestRuntimePermissionLocation());
        //requestRuntimePermissionNotification();

        /*** Preferenze intervallo misurazione ***/
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        this.measurementInterval = preferences.getString("measurementInterval", "5s");

        startMeasure = findViewById(R.id.startMeasure);
        startMeasure.setOnClickListener(v -> startMeasurement());


        /*** Gestore bottone impostazioni ***/
        ImageButton buttonOptions = findViewById(R.id.btnOptions);
        buttonOptions.setOnClickListener(v -> {
            Intent optionsIntent = new Intent(MainActivity.this, OptionsActivity.class);
            startActivity(optionsIntent);
        });

    }



    private void startMeasurement() {
        // Altrimenti, avvia la misurazione
        if (lteCheckBox.isChecked()) {
            updateLteLevel();
        } else if (wifiCheckBox.isChecked()) {
            updateWifiLevel();
        } else if (rumoreCheckBox.isChecked()) {
            updateNoiseLevel();
        } else {
            showToast("Seleziona un tipo di misurazione");
        }
    }


    void updateLteLevel(){
        if(currentLocation != null) {
            LteSignalManager lteSignalManager = new LteSignalManager(MainActivity.this, currentLocation, myMap);
            showToast(LteSignalManager.insertLTEMeasurement(currentLocation, getIntervalInMillis(measurementInterval)));
            lteSignalManager.showLteMap();
        } else {
            showToast("Posizione non disponibile. ");
        }
    }

    void updateWifiLevel(){
        // Chiamata al metodo updateLteLevel()
        if(currentLocation != null) {
            WifiSignalManager wifiSignalManager = new WifiSignalManager(MainActivity.this, currentLocation, myMap);
            showToast(WifiSignalManager.insertWifiMeasurement(currentLocation, getIntervalInMillis(measurementInterval)));
            WifiSignalManager.showWifiMap();
        } else {
            showToast("Posizione non disponibile.");
        }
    }


    void updateNoiseLevel(){
        if(currentLocation != null) {

            handler.removeCallbacks(updateLteText);
            handler.removeCallbacks(updateWifiText);
            findViewById(R.id.ltelegend).setVisibility(View.INVISIBLE);
            findViewById(R.id.noiselegend).setVisibility(View.VISIBLE);
            findViewById(R.id.wifilegend).setVisibility(View.INVISIBLE);
            NoiseSignalManager noiseSignalManager = new NoiseSignalManager(MainActivity.this, currentLocation, myMap);
            showToast(NoiseSignalManager.insertNoiseMeasurement(currentLocation, getIntervalInMillis(measurementInterval)));
            noiseSignalManager.showNoiseMap();

            handler.post(updateNoiseText);

        } else {
            showToast("Posizione non disponibile.");
        }
    }


    // Runnable per aggiornare il testo della TextView ogni secondo
    private final Runnable updateLteText = new Runnable() {
        @Override
        public void run() {
            // Aggiorna il testo della TextView con il valore da lteSignalManager.getLTELevel()
            variableText.setText("Lte : " + LteSignalManager.getLTELevel() + " dBm");
            // Esegui questo Runnable dopo 1 secondo
            handler.postDelayed(this, 1000);
        }
    };

    // Runnable per aggiornare il testo della TextView ogni secondo
    private final Runnable updateWifiText = new Runnable() {
        @Override
        public void run() {
            // Aggiorna il testo della TextView con il valore da lteSignalManager.getLTELevel()
            variableText.setText("Wifi : " + WifiSignalManager.getWifiLevel() + " Mb/s");
            // Esegui questo Runnable dopo 1 secondo
            handler.postDelayed(this, 1000);
        }
    };

    // Runnable per aggiornare il testo della TextView ogni secondo
    private final Runnable updateNoiseText = new Runnable() {
        @Override
        public void run() {
            // Aggiorna il testo della TextView con il valore da lteSignalManager.getLTELevel()
            variableText.setText("Noise : " + NoiseSignalManager.getNoiseLevel() + " dB/s");
            // Esegui questo Runnable dopo 1 secondo
            handler.postDelayed(this, 1000);
        }
    };

    private void requestRuntimePermissionLocation() {
        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // Se l'autorizzazione è già concessa, ottieni la posizione
            getLocation();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_LOCATION_PERMISSION)) {
            // Spiega l'importanza dell'autorizzazione all'utente
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Questa app richiede l'autorizzazione per la posizione per mostrare le funzionalità.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{FINE_LOCATION_PERMISSION},
                                PERMISSION_LOCATION_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    });

            builder.show();
        } else {
            // Richiedi l'autorizzazione
            ActivityCompat.requestPermissions(this, new String[]{FINE_LOCATION_PERMISSION}, PERMISSION_LOCATION_CODE);
        }
    }



    private void requestRuntimePermissionAudio() {
        if (ActivityCompat.checkSelfPermission(this, RECORD_AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted. ", Toast.LENGTH_SHORT).show();
            handler.removeCallbacks(updateLteText);
            handler.removeCallbacks(updateWifiText);

            findViewById(R.id.ltelegend).setVisibility(View.INVISIBLE);
            findViewById(R.id.noiselegend).setVisibility(View.VISIBLE);
            findViewById(R.id.wifilegend).setVisibility(View.INVISIBLE);
            noiseSignalManager = new NoiseSignalManager(this, myMap);
            noiseSignalManager.showNoiseMap();
            handler.post(updateNoiseText);

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO_PERMISSION)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Questa app richiede il permesso per il microfono per rilevare il rumore circostante.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO_PERMISSION},
                                PERMISSION_AUDIO_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", ((dialog, which) -> dialog.dismiss())
                    );

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO_PERMISSION}, PERMISSION_AUDIO_CODE);
        }
    }


    private void requestRuntimePermissionNotification() {
        if (ActivityCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted. ", Toast.LENGTH_SHORT).show();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, NOTIFICATION_PERMISSION)) {
            // Spiega l'importanza dell'autorizzazione all'utente
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Questa app richiede l'autorizzazione per le notifiche.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{NOTIFICATION_PERMISSION},
                                PERMISSION_NOTIFICATION_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    });

            builder.show();
        } else {
            // Richiedi l'autorizzazione
            Log.d("prova","entro notifiche ELSE");
            ActivityCompat.requestPermissions(this, new String[]{NOTIFICATION_PERMISSION}, PERMISSION_NOTIFICATION_CODE);
            //requestPermissions(new String[]{NOTIFICATION_PERMISSION}, PERMISSION_NOTIFICATION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_NOTIFICATION_CODE) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, NOTIFICATION_PERMISSION)) {
                // L'utente ha negato l'autorizzazione in modo permanente, mostra un messaggio
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Questa app richiede l'autorizzazione per le notifiche.")
                        .setTitle("Permission Required")
                        .setCancelable(false)
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                builder.show();
            } else {
                // L'utente ha negato l'autorizzazione, richiedi di nuovo
                requestRuntimePermissionNotification();
            }
        } else if (requestCode == PERMISSION_LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Se l'autorizzazione è stata concessa, ottieni la posizione
                getLocation();
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_LOCATION_PERMISSION)) {
                // L'utente ha negato l'autorizzazione in modo permanente, mostra un messaggio
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Questa app richiede l'autorizzazione per la posizione per mostrare le funzionalità.")
                        .setTitle("Permission Required")
                        .setCancelable(false)
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

                builder.show();
            } else {
                // L'utente ha negato l'autorizzazione, richiedi di nuovo
                requestRuntimePermissionLocation();
            }
        }
        /*** Richiesta autorizzazione per l'audio ***/
        else if (requestCode == PERMISSION_AUDIO_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted. ", Toast.LENGTH_SHORT).show();

                handler.removeCallbacks(updateLteText);
                handler.removeCallbacks(updateWifiText);

                findViewById(R.id.wifilegend).setVisibility(View.INVISIBLE);
                findViewById(R.id.ltelegend).setVisibility(View.INVISIBLE);
                findViewById(R.id.noiselegend).setVisibility(View.VISIBLE);

                //richiesta per misurazione in background
                noiseSignalManager = new NoiseSignalManager(this, currentLocation, myMap);
                noiseSignalManager.showNoiseMap();
                handler.post(updateNoiseText);

            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO_PERMISSION)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Questa app richiede il permesso per il microfono per rilevare il rumore circostante.")
                        .setTitle("Permission Required")
                        .setCancelable(false)
                        .setPositiveButton("Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);

                            dialog.dismiss();
                        })
                        .setNegativeButton("Cancel", ((dialog, which) -> dialog.dismiss()));

                builder.show();
            } else {
                requestRuntimePermissionAudio();
            }
        } 

    }

    private void showToastZoom() {
        if(myMap != null) {
            currentZoom = myMap.getCameraPosition().zoom;

            if (GridManager.getInstance().isVisible() && currentZoom < 4) {
                // Mostra un Toast per aumentare lo zoom
                showToast("Aumentare lo zoom per visualizzare la griglia correttamente");
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        // Imposta il massimo livello di zoom a 16
        myMap.setMaxZoomPreference(16);

        // Utilizza il valore selezionato dallo spinner per impostare il tipo di mappa
        SharedPreferences preferencesMap = getSharedPreferences("PrefMap", MODE_PRIVATE);
        String selectedMapType = preferencesMap.getString("selectedMapType", "Normal");
        changeMapType(selectedMapType);

        // Posiziona la mappa sulla posizione corrente
        if (currentLatLng != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                myMap.setMyLocationEnabled(true);
            }
        }

    }

    private void changeMapType(String mapType) {
        if (mapType.equals("Satellite")) {
            myMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        } else {
            myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLocation();
        if (myMap != null) {
            onMapReady(myMap);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (myMap != null) {
            onMapReady(myMap);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        createWorker();
    }


    private void getLocation() {
        // Ottieni l'ultima posizione concesso

        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    // Aggiorna la mappa con la posizione corrente
                    currentLocation = location;
                    currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    onMapReady(myMap);

                } else {
                    Toast.makeText(MainActivity.this, "Posizione non disponibile.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Metodo per ottenere l'intervallo in millisecondi
    private long getIntervalInMillis(String time) {
        long interval = 5000;  // Default a 5 secondi

        // Conversione dell'intervallo da stringa a millisecondi
        if (time.endsWith("s")) {
            interval = Long.parseLong(time.replace("s", "")) * 1000;
        } else if (time.endsWith("m")) {
            interval = Long.parseLong(time.replace("m", "")) * 60 * 1000;
        }

        return interval;
    }

    private void createWorker(){
        Constraints constraints = new Constraints.Builder().setRequiresBatteryNotLow(true).build();

        final PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
                NotificationWorker.class,
                15,
                TimeUnit.MINUTES)
                //.setInitialDelay(6000,TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();

        WorkManager workManager =  WorkManager.getInstance(this);

        workManager.enqueue(periodicWorkRequest);

        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(@Nullable WorkInfo workInfo) {
                        if (workInfo != null) {
                            Log.d("prova", "Status changed to : " + workInfo.getState());

                        }
                    }
                });
    }

}
