package com.example.labappmobili;


import static com.example.labappmobili.GridTileProvider.inMetersLatCoordinate;
import static com.example.labappmobili.GridTileProvider.inMetersLngCoordinate;
import static com.example.labappmobili.LteSignalManager.deleteLTEMeasurement;
import static com.example.labappmobili.LteSignalManager.showLteMap;
import static com.example.labappmobili.NoiseSignalManager.deleteNoiseMeasurement;
import static com.example.labappmobili.NoiseSignalManager.showNoiseMap;
import static com.example.labappmobili.WifiSignalManager.deleteWifiMeasurement;
import static com.example.labappmobili.WifiSignalManager.showWifiMap;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.Noise.Noise;
import com.example.labappmobili.RoomDB.WiFi.WiFi;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final int PERMISSION_LOCATION_CODE = 1;
    public static final String FINE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION;

    public static final int PERMISSION_AUDIO_CODE = 2;
    public static final String RECORD_AUDIO_PERMISSION = android.Manifest.permission.RECORD_AUDIO;

    public static final int PERMISSION_NOTIFICATION_CODE = 3;
    public static final String NOTIFICATION_PERMISSION = android.Manifest.permission.POST_NOTIFICATIONS;

    public static final int PERMISSION_BACKGROUND_CODE = 4;
    public static final String BACKGROUND_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION;

    private WifiSignalManager wifiSignalManager;
    private NoiseSignalManager noiseSignalManager;
    private SearchView mapSearchView;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleMap myMap;
    LatLng currentLatLng;

    private TextView variableText;

    CheckBox lteCheckBox, wifiCheckBox, noiseCheckBox;
    static boolean isWifiEnabled = true;
    static boolean isLteEnabled = true;

    Button startMeasure;

    // Dichiarazione del tuo handler
    private final Handler handler = new Handler();

    // Dichiarazione della tua variabile per l'intervallo di tempo
    static String measurementInterval = "5s";  // Default a 5 secondi

    float currentZoom;

    boolean showLte = false;
    boolean showWifi = false;
    boolean showNoise = false;

    private LineChart lineChart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapSearchView = findViewById(R.id.mapSearch);
        variableText = findViewById(R.id.variableText);

        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        createWorker();


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
        noiseCheckBox = findViewById(R.id.noiseCheckBox);


        // Aggiungi un listener per gestire gli eventi di selezione
        CheckBox.OnCheckedChangeListener checkBoxListener = (buttonView, isChecked) -> {

            if (isChecked) {
                    // Aggiungi le azioni da eseguire quando un CheckBox viene selezionato
                int checkBoxId = buttonView.getId();

                if (checkBoxId == R.id.lteCheckBox) {
                    wifiCheckBox.setChecked(false);
                    noiseCheckBox.setChecked(false);
                    // Azioni per la selezione di LTE
                    isLteEnabled = preferences.getBoolean("isLteEnabled", true);
                    if (isLteEnabled) {
                        handler.removeCallbacks(updateWifiText);
                        handler.removeCallbacks(updateNoiseText);

                        findViewById(R.id.ltelegend).setVisibility(View.VISIBLE);
                        findViewById(R.id.noiselegend).setVisibility(View.INVISIBLE);
                        findViewById(R.id.wifilegend).setVisibility(View.INVISIBLE);

                        LteSignalManager lteSignalManager = new LteSignalManager(MainActivity.this, myMap);
                        lteSignalManager.showLteMap();

                        showLte = true;
                        showWifi = false;
                        showNoise = false;

                        handler.post(updateLteText);

                    } else {
                        Toast.makeText(this, R.string.permission_denied , Toast.LENGTH_LONG).show();
                        buttonView.setChecked(false);
                    }
                }

                else if (checkBoxId == R.id.wifiCheckBox) {
                    lteCheckBox.setChecked(false);
                    noiseCheckBox.setChecked(false);
                    // Azioni per la selezione di WiFi
                    isWifiEnabled = preferences.getBoolean("isWifiEnabled", true);
                    if (isWifiEnabled) {
                        handler.removeCallbacks(updateLteText);
                        handler.removeCallbacks(updateNoiseText);

                        findViewById(R.id.ltelegend).setVisibility(View.INVISIBLE);
                        findViewById(R.id.noiselegend).setVisibility(View.INVISIBLE);
                        findViewById(R.id.wifilegend).setVisibility(View.VISIBLE);

                        wifiSignalManager = new WifiSignalManager(MainActivity.this, myMap);
                        showWifiMap();

                        showLte = false;
                        showWifi = true;
                        showNoise = false;

                        handler.post(updateWifiText);
                    } else {
                        Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
                        buttonView.setChecked(false);
                    }
                }

                else if (checkBoxId == R.id.noiseCheckBox) {
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

                showLte = false;
                showWifi = false;
                showNoise = false;

                variableText.setText(R.string.variableText_default);  // Pulisci il testo
                }

        };

        // Aggiungi il listener ai CheckBox
        lteCheckBox.setOnCheckedChangeListener(checkBoxListener);
        wifiCheckBox.setOnCheckedChangeListener(checkBoxListener);
        noiseCheckBox.setOnCheckedChangeListener(checkBoxListener);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        /*** Inizializza la mappa ***/
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        /*** Richiedi l'autorizzazione per la posizione ***/
        findViewById(R.id.my_location).setOnClickListener(v -> requestRuntimePermissionLocation());

        /*** Preferenze intervallo misurazione ***/
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
        } else if (noiseCheckBox.isChecked()) {
            updateNoiseLevel();
        } else {
            showToast(this.getResources().getString(R.string.select_measure));
        }
    }


    void updateLteLevel(){
        if(currentLocation != null) {
            LteSignalManager lteSignalManager = new LteSignalManager(MainActivity.this, currentLocation, myMap);
            showToast(LteSignalManager.insertLTEMeasurement(currentLocation, getIntervalInMillis(measurementInterval)));
            lteSignalManager.showLteMap();
        } else {
            showToast(this.getResources().getString(R.string.location_not_found));
        }
    }

    void updateWifiLevel(){
        // Chiamata al metodo updateLteLevel()
        if(currentLocation != null) {
            WifiSignalManager wifiSignalManager = new WifiSignalManager(MainActivity.this, currentLocation, myMap);
            showToast(WifiSignalManager.insertWifiMeasurement(currentLocation, getIntervalInMillis(measurementInterval)));
            showWifiMap();
        } else {
            showToast(this.getResources().getString(R.string.location_not_found));
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
            showNoiseMap();

            showLte = false;
            showWifi = false;
            showNoise = true;

            handler.post(updateNoiseText);

        } else {
            showToast(this.getResources().getString(R.string.location_not_found));
        }
    }


    // Runnable per aggiornare il testo della TextView ogni secondo
    private final Runnable updateLteText = new Runnable() {
        @Override
        public void run() {
            // Aggiorna il testo della TextView con il valore da lteSignalManager.getLTELevel()
            variableText.setText(getResources().getString(R.string.lte)+ " : " + LteSignalManager.getLTELevel() + " dBm");
            // Esegui questo Runnable dopo 1 secondo
            handler.postDelayed(this, 1000);
        }
    };

    // Runnable per aggiornare il testo della TextView ogni secondo
    private final Runnable updateWifiText = new Runnable() {
        @Override
        public void run() {
            // Aggiorna il testo della TextView con il valore da lteSignalManager.getLTELevel()
            variableText.setText(getResources().getString(R.string.wifi)+ " : " + WifiSignalManager.getWifiLevel() + " Mb/s");
            // Esegui questo Runnable dopo 1 secondo
            handler.postDelayed(this, 1000);
        }
    };

    // Runnable per aggiornare il testo della TextView ogni secondo
    private final Runnable updateNoiseText = new Runnable() {
        @Override
        public void run() {
            // Aggiorna il testo della TextView con il valore da lteSignalManager.getLTELevel()
            variableText.setText(getResources().getString(R.string.noise)+ " : " + + NoiseSignalManager.getNoiseLevel() + " dB/s");
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
            builder.setMessage(R.string.message_position)
                    .setTitle(R.string.permission_required)
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{FINE_LOCATION_PERMISSION},
                                PERMISSION_LOCATION_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
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
            handler.removeCallbacks(updateLteText);
            handler.removeCallbacks(updateWifiText);

            findViewById(R.id.ltelegend).setVisibility(View.INVISIBLE);
            findViewById(R.id.noiselegend).setVisibility(View.VISIBLE);
            findViewById(R.id.wifilegend).setVisibility(View.INVISIBLE);
            noiseSignalManager = new NoiseSignalManager(this, myMap);
            showNoiseMap();

            showLte = false;
            showWifi = false;
            showNoise = true;

            handler.post(updateNoiseText);

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO_PERMISSION)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.message_microphone)
                    .setTitle(R.string.permission_required)
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO_PERMISSION},
                                PERMISSION_AUDIO_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, ((dialog, which) -> dialog.dismiss())
                    );

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO_PERMISSION}, PERMISSION_AUDIO_CODE);
        }
    }


    private void requestRuntimePermissionNotification() {
        if (ActivityCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, NOTIFICATION_PERMISSION)) {
            // Spiega l'importanza dell'autorizzazione all'utente
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.message_notifications)
                    .setTitle(R.string.permission_required)
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{NOTIFICATION_PERMISSION},
                                PERMISSION_NOTIFICATION_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        dialog.dismiss();
                    });

            builder.show();
        } else {
            // Richiedi l'autorizzazione
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
                builder.setMessage(R.string.message_notifications)
                        .setTitle(R.string.permission_required)
                        .setCancelable(false)
                        .setPositiveButton(R.string.setting, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

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
                builder.setMessage(R.string.message_position)
                        .setTitle(R.string.permission_required)
                        .setCancelable(false)
                        .setPositiveButton(R.string.setting, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

                builder.show();
            } else {
                // L'utente ha negato l'autorizzazione, richiedi di nuovo
                requestRuntimePermissionLocation();
            }
        }
        /*** Richiesta autorizzazione per l'audio ***/
        else if (requestCode == PERMISSION_AUDIO_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();

                handler.removeCallbacks(updateLteText);
                handler.removeCallbacks(updateWifiText);

                findViewById(R.id.wifilegend).setVisibility(View.INVISIBLE);
                findViewById(R.id.ltelegend).setVisibility(View.INVISIBLE);
                findViewById(R.id.noiselegend).setVisibility(View.VISIBLE);

                //richiesta per misurazione in background
                noiseSignalManager = new NoiseSignalManager(this, currentLocation, myMap);
                showNoiseMap();

                showLte = false;
                showWifi = false;
                showNoise = true;

                handler.post(updateNoiseText);

            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO_PERMISSION)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.message_microphone)
                        .setTitle(R.string.permission_required)
                        .setCancelable(false)
                        .setPositiveButton(R.string.setting, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);

                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, ((dialog, which) -> dialog.dismiss()));

                builder.show();
            } else {
                requestRuntimePermissionAudio();
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

        // Aggiungi l'ascoltatore di tocco per il tocco di lunga durata
        myMap.setOnMapLongClickListener(latLng -> {
            if(showLte || showWifi || showNoise)
                showObjectListDialog(inMetersLatCoordinate(latLng.latitude), inMetersLngCoordinate(latLng.longitude));
        });

        // Posiziona la mappa sulla posizione corrente
        if (currentLatLng != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                myMap.setMyLocationEnabled(true);
            }
        }

    }


    private void showObjectListDialog(double latitudine, double longitudine) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_layout);

        List<?> objectList = GridTileProvider.getListMapTouch(latitudine, longitudine);

        RecyclerView recyclerView = bottomSheetDialog.findViewById(R.id.recyclerView);
        TextView noMeasurementText = bottomSheetDialog.findViewById(R.id.noMeasurementText);
        lineChart = bottomSheetDialog.findViewById(R.id.lineChart);
        ImageButton imageButton = bottomSheetDialog.findViewById(R.id.imageButton);
        TextView title = bottomSheetDialog.findViewById(R.id.title);
        TextView subtitle = bottomSheetDialog.findViewById(R.id.subtitle);


        if (recyclerView != null) {
            if(objectList.isEmpty()) {
                // Nascondi il RecyclerView e mostra il messaggio quando la lista è vuota
                recyclerView.setVisibility(View.GONE);
                imageButton.setVisibility(View.GONE);
                title.setVisibility(View.GONE);
                subtitle.setVisibility(View.GONE);


                if (noMeasurementText != null) {
                    noMeasurementText.setVisibility(View.VISIBLE);
                    noMeasurementText.setText(this.getResources().getString(R.string.bottom_nomeasure));
                }
            }else{

                MyAdapter adapter = new MyAdapter(objectList, item -> {

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(this.getResources().getString(R.string.bottom_dialog_title));
                    builder.setMessage(this.getResources().getString(R.string.bottom_dialog_text));

                    builder.setPositiveButton(this.getResources().getString(R.string.confirm), (dialog, which) -> {

                        if (showLte) {
                            deleteLTEMeasurement(item);
                            showLteMap();
                        } else if (showWifi) {
                            deleteWifiMeasurement(item);
                            showWifiMap();
                        } else {
                            deleteNoiseMeasurement(item);
                            showNoiseMap();
                        }

                        dialog.dismiss();
                        bottomSheetDialog.dismiss();

                        showToast(this.getResources().getString(R.string.measure_delete));
                    });

                    builder.setNegativeButton(this.getResources().getString(R.string.cancel), (dialog, which) -> {
                        // Chiudi la finestra di dialogo senza eliminare la misurazione
                        dialog.dismiss();
                    });

                    builder.show();
                });

                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(this));
            }
        }

        if (imageButton != null) {
            imageButton.setOnClickListener(v -> {
                if (lineChart.getVisibility() == View.VISIBLE ) {
                    if (lineChart != null) {
                        lineChart.setVisibility(View.GONE);
                    }
                } else {
                    showLineChart(objectList);
                }
            });
        }

        bottomSheetDialog.show();
    }


    private void showLineChart(List<?> objectList) {
        if (lineChart != null && objectList != null && objectList.size() > 0) {
            lineChart.setVisibility(View.VISIBLE);
            lineChart.setDrawGridBackground(false);

            YAxis yAxis = lineChart.getAxisLeft();
            YAxis rightYAxis = lineChart.getAxisRight();
            rightYAxis.setDrawAxisLine(false);
            rightYAxis.setDrawLabels(false);

            List<Entry> entries = new ArrayList<>();
            Description description = new Description();

            LineDataSet dataSet = null;

            for (int i = 0; i < objectList.size(); i++) {
                Object measure = objectList.get(i);
                float yValue;

                if (showLte) {
                    yValue = ((LTE) measure).getLteValue();
                    yAxis.setAxisMaximum(5f);
                    yAxis.setLabelCount(objectList.size());
                    description.setText(this.getResources().getString(R.string.lte));
                } else if (showWifi) {
                    yValue = (float) ((WiFi) measure).getWiFiValue();
                    yAxis.setAxisMaximum(100f);
                    description.setText(this.getResources().getString(R.string.wifi));
                } else {
                    yValue = (float) ((Noise) measure).getNoiseValue();
                    yAxis.setAxisMaximum(100f);
                    description.setText(this.getResources().getString(R.string.noise));
                }

                entries.add(new Entry(i, yValue));
                dataSet = new LineDataSet(entries, description.getText().toString());
            }

            lineChart.setDescription(description);

            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setLabelCount(objectList.size());
            xAxis.setGranularity(1f);

            if (dataSet != null) {
                dataSet.setColor(Color.BLUE);

                LineData lineData = new LineData(dataSet);
                lineChart.setData(lineData);
                lineChart.invalidate();
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
                    Toast.makeText(MainActivity.this, R.string.location_not_found, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Metodo per ottenere l'intervallo in millisecondi
    static long getIntervalInMillis(String time) {
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
                BackgroundWorker.class,15, TimeUnit.MINUTES)
                .setInitialDelay(6000 ,TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();

        WorkManager workManager =  WorkManager.getInstance(this);

        workManager.enqueue(periodicWorkRequest);

        workManager.getWorkInfoByIdLiveData(periodicWorkRequest.getId())
                .observe(this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(@Nullable WorkInfo workInfo) {
                        if (workInfo != null) {
                            Log.d("BackgroundWorker", "Status changed to : " + workInfo.getState());
                        }
                    }
                });
    }


    public static void stopWorker() {
        WorkManager.getInstance().cancelAllWork();
    }

}
