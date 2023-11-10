package com.example.labappmobili;


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
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final int PERMISSION_LOCATION_CODE = 1;
    public static final String FINE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION;

    public static final int PERMISSION_AUDIO_CODE = 2;
    public static final String RECORD_AUDIO_PERMISSION = android.Manifest.permission.RECORD_AUDIO;

    public static final int PERMISSION_NOTIFICATION_CODE = 3;
    public static final String NOTIFICATION_PERMISSION = android.Manifest.permission.POST_NOTIFICATIONS;

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

    Button startMeasure, stopMeasure;


    // Dichiarazione del tuo handler
    private final Handler handler = new Handler();

    // Dichiarazione della tua variabile per l'intervallo di tempo
    private String measurementInterval = "5s";  // Default a 5 secondi
    private String selectedMapType = "Normal";


    private boolean isMeasuring = false;


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

        stopMeasure = findViewById(R.id.stopMeasure);


        // Aggiungi un listener per gestire gli eventi di selezione
        CheckBox.OnCheckedChangeListener checkBoxListener = (buttonView, isChecked) -> {

            if (isChecked && !isMeasuring) {

                    // Aggiungi le azioni da eseguire quando un CheckBox viene selezionato
                    int checkBoxId = buttonView.getId();

                    if (checkBoxId == R.id.lteCheckBox) {
                        wifiCheckBox.setChecked(false);
                        rumoreCheckBox.setChecked(false);
                        // Azioni per la selezione di LTE
                        if (isLteEnabled) {
                            variableText.setText("Misurazione in corso . . .");
                            LteSignalManager lteSignalManager = new LteSignalManager(MainActivity.this, myMap);
                            lteSignalManager.updateLTELevel();

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
                            variableText.setText("Misurazione in corso . . .");
                            wifiSignalManager = new WifiSignalManager(MainActivity.this, myMap);
                            wifiSignalManager.updateWifiSignalStrength();
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
                } else if(isChecked && isMeasuring) {
                    showToast("Interrompere la misurazione in corso prima di selezionare un'altra opzione.");
                    buttonView.setChecked(false);  // Annulla la selezione
                } else if(!isChecked && isMeasuring){
                    showToast("Interrompere la misurazione in corso prima di deselezionare la misurazione.");
                    buttonView.setChecked(true);  // Annulla la selezione
                } else {
                    // Se è in corso una misurazione, arrestala
                    handler.removeCallbacks(updateLteLevelRunnable);
                    handler.removeCallbacks(updateWifiLevelRunnable);
                    handler.removeCallbacks(updateRumoreLevelRunnable);
                    isMeasuring = false;

                    startMeasure.setVisibility(View.VISIBLE);
                    stopMeasure.setVisibility(View.INVISIBLE);

                    GridManager.getInstance().removeGrid();
                    //showToast("Errore generale");
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
        findViewById(R.id.my_location).setOnClickListener(v -> requestRuntimePermissionLocation());


        /*** Preferenze intervallo misurazione ***/
        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        this.measurementInterval = preferences.getString("measurementInterval", "5s");

        startMeasure = findViewById(R.id.startMeasure);
        startMeasure.setOnClickListener(v -> startMeasurement());


        stopMeasure = findViewById(R.id.stopMeasure);
        stopMeasure.setOnClickListener(v ->  stopMeasurement());


        /*** Gestore bottone impostazioni ***/
        ImageButton buttonOptions = findViewById(R.id.btnOptions);
        buttonOptions.setOnClickListener(v -> {
            Intent optionsIntent = new Intent(MainActivity.this, OptionsActivity.class);
            startActivity(optionsIntent);
        });


    }

    private void startMeasurement() {
        if (isMeasuring) {
            // Se è in corso una misurazione, arrestala
            stopMeasurement();
        } else {
            // Altrimenti, avvia la misurazione
            if (lteCheckBox.isChecked()) {
                isMeasuring = true;
                handler.post(updateLteLevelRunnable);
            } else if (wifiCheckBox.isChecked()) {
                isMeasuring = true;
                handler.post(updateWifiLevelRunnable);
            } else if (rumoreCheckBox.isChecked()) {
                isMeasuring = true;
                handler.post(updateRumoreLevelRunnable);
            } else {
                showToast("Seleziona un tipo di misurazione");
                // Imposta la visibilità dei bottoni
                startMeasure.setVisibility(View.VISIBLE);
                stopMeasure.setVisibility(View.INVISIBLE);
                isMeasuring = false;
            }

        }
    }

    private void stopMeasurement() {
        // Interrompi la misurazione
        handler.removeCallbacks(updateLteLevelRunnable);
        handler.removeCallbacks(updateWifiLevelRunnable);
        handler.removeCallbacks(updateRumoreLevelRunnable);

        isMeasuring = false;

        // Imposta la visibilità dei bottoni
        startMeasure.setVisibility(View.VISIBLE);
        stopMeasure.setVisibility(View.INVISIBLE);

        variableText.setText("VISUALIZZA MISURAZIONI...");  // Pulisci il testo
        showToast("Misurazione interrotta");
    }


    // Dichiarazione del runnable per eseguire l'azione periodica
    private final Runnable updateLteLevelRunnable = new Runnable() {
        @Override
        public void run() {
            // Chiamata al metodo updateLteLevel()
            if(currentLocation != null) {
                startMeasure.setVisibility(View.INVISIBLE);
                stopMeasure.setVisibility(View.VISIBLE);

                showToast("Misurazione in corso...");

                LteSignalManager lteSignalManager = new LteSignalManager(MainActivity.this, currentLocation, myMap);
                lteSignalManager.updateLTELevel();
            } else {
                showToast("Posizione non disponibile. ");
                handler.removeCallbacks(this);
                isMeasuring = false;
                // Imposta la visibilità dei bottoni
                startMeasure.setVisibility(View.VISIBLE);
                stopMeasure.setVisibility(View.INVISIBLE);
                return;
            }

            // Pianifica il prossimo aggiornamento dopo l'intervallo specificato
            handler.postDelayed(this, getIntervalInMillis());
        }
    };



    private final Runnable updateWifiLevelRunnable = new Runnable() {
        @Override
        public void run() {
            // Chiamata al metodo updateLteLevel()
            if(currentLocation != null) {
                startMeasure.setVisibility(View.INVISIBLE);
                stopMeasure.setVisibility(View.VISIBLE);

                showToast("Misurazione in corso...");

                WifiSignalManager wifiSignalManager = new WifiSignalManager(MainActivity.this, currentLocation, myMap);
                wifiSignalManager.updateWifiSignalStrength();
            } else {
                showToast("Posizione non disponibile.");
                handler.removeCallbacks(this);
                isMeasuring = false;
                startMeasure.setVisibility(View.VISIBLE);
                stopMeasure.setVisibility(View.INVISIBLE);
                return;
            }

            // Pianifica il prossimo aggiornamento dopo l'intervallo specificato
            handler.postDelayed(this, getIntervalInMillis());
        }
    };


    private final Runnable updateRumoreLevelRunnable = new Runnable() {
        @Override
        public void run() {
            // Chiamata al metodo updateLteLevel()
            if(currentLocation != null) {
                startMeasure.setVisibility(View.INVISIBLE);
                stopMeasure.setVisibility(View.VISIBLE);

                showToast("Misurazione in corso...");

                NoiseSignalManager noiseSignalManager = new NoiseSignalManager(MainActivity.this, currentLocation, myMap);
                noiseSignalManager.updateNoiseLevel();
            } else {
                showToast("Posizione non disponibile.");
                handler.removeCallbacks(this);
                isMeasuring = false;
                startMeasure.setVisibility(View.VISIBLE);
                stopMeasure.setVisibility(View.INVISIBLE);
                return;
            }

            // Pianifica il prossimo aggiornamento dopo l'intervallo specificato
            handler.postDelayed(this, getIntervalInMillis());
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

            //TODO mettere controllo localizzazione prima della richiesta dell'audio.
            noiseSignalManager = new NoiseSignalManager(this, myMap);
            //richiesta per controllo misurazione in background
            noiseSignalManager.updateNoiseLevel();

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_LOCATION_CODE) {
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

                //TODO mettere controllo localizzazione prima della richiesta dell'audio.
                noiseSignalManager = new NoiseSignalManager(this, myMap);

                //richiesta per misurazione in background
                //noiseSignalManager = new NoiseSignalManager(this, currentLocation, myMap);
                noiseSignalManager.updateNoiseLevel();

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



        if (isMeasuring) {
            startMeasure.setVisibility(View.INVISIBLE);
            stopMeasure.setVisibility(View.VISIBLE);
        } else {
            startMeasure.setVisibility(View.VISIBLE);
            stopMeasure.setVisibility(View.INVISIBLE);
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
        if (isMeasuring) {
            stopMeasurement();
        }
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
    private long getIntervalInMillis() {
        long interval = 5000;  // Default a 5 secondi

        // Conversione dell'intervallo da stringa a millisecondi
        if (measurementInterval.endsWith("s")) {
            interval = Long.parseLong(measurementInterval.replace("s", "")) * 1000;
        } else if (measurementInterval.endsWith("m")) {
            interval = Long.parseLong(measurementInterval.replace("m", "")) * 60 * 1000;
        }

        return interval;
    }
}
