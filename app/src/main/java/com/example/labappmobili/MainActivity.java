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


    // Dichiarazione del tuo handler
    private final Handler handler = new Handler();

    // Dichiarazione della tua variabile per l'intervallo di tempo
    private String measurementInterval = "5s";  // Default a 5 secondi
    private String selectedMapType;


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


        SharedPreferences preferencesMap = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        this.selectedMapType = preferences.getString("selectedMapType", "Normal");


        /*** Listener per bottone misurazioni ***/
        findViewById(R.id.misura).setOnClickListener(v -> {

            if (isMeasuring) {
                // Se è in corso una misurazione, arrestala
                handler.removeCallbacks(updateLteLevelRunnable);
                handler.removeCallbacks(updateWifiLevelRunnable);
                handler.removeCallbacks(updateRumoreLevelRunnable);

                isMeasuring = false;
                showToast("Misurazione interrotta");
                variableText.setText("VISUALIZZA MISURAZIONI...");  // Pulisci il testo
            } else {
                // Altrimenti, avvia la misurazione
                if (lteCheckBox.isChecked()) {
                    isMeasuring = true;
                    showToast("Misurazione Lte in corso...");
                    handler.post(updateLteLevelRunnable);
                } else if (wifiCheckBox.isChecked()) {
                    // Esegui azioni per misurazione WiFi
                    isMeasuring = true;
                    showToast("Misurazione WiFi in corso...");
                    handler.post(updateWifiLevelRunnable);
                } else if (rumoreCheckBox.isChecked()) {
                    // Esegui azioni per misurazione Rumore
                    isMeasuring = true;
                    showToast("Misurazione Rumore in corso...");
                    handler.post(updateRumoreLevelRunnable);
                } else {
                    showToast("Seleziona un tipo di misurazione");
                    isMeasuring = false;
                }
            }

        });


        /*** Gestore bottone impostazioni ***/
        ImageButton buttonOptions = findViewById(R.id.btnOptions);
        buttonOptions.setOnClickListener(v -> {
            Intent optionsIntent = new Intent(MainActivity.this, OptionsActivity.class);
            startActivity(optionsIntent);
        });


    }



    // Dichiarazione del runnable per eseguire l'azione periodica
    private final Runnable updateLteLevelRunnable = new Runnable() {
        @Override
        public void run() {
            // Chiamata al metodo updateLteLevel()
            if(currentLocation != null) {
                LteSignalManager lteSignalManager = new LteSignalManager(MainActivity.this, currentLocation, myMap);
                lteSignalManager.updateLTELevel();
            } else {
                showToast("Posizione non disponibile.");
                handler.removeCallbacks(this);
                isMeasuring = false;
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
                WifiSignalManager wifiSignalManager = new WifiSignalManager(MainActivity.this, currentLocation, myMap);
                wifiSignalManager.updateWifiSignalStrength();
            } else {
                showToast("Posizione non disponibile.");
                handler.removeCallbacks(this);
                isMeasuring = false;
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
                NoiseSignalManager noiseSignalManager = new NoiseSignalManager(MainActivity.this, currentLocation, myMap);
                noiseSignalManager.updateNoiseLevel();
            } else {
                showToast("Posizione non disponibile.");
                handler.removeCallbacks(this);
                isMeasuring = false;
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

        if(selectedMapType == "Satellite") {
            myMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }else {
            myMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }

        // Posiziona la mappa sulla posizione corrente
        if (currentLatLng != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                myMap.setMyLocationEnabled(true);
            }
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
        // Se è in corso una misurazione, arrestala
        handler.removeCallbacks(updateLteLevelRunnable);
        handler.removeCallbacks(updateWifiLevelRunnable);
        handler.removeCallbacks(updateRumoreLevelRunnable);
        if(isMeasuring) {
            isMeasuring = false;
            showToast("Misurazione interrotta");
        }
        variableText.setText("VISUALIZZA MISURAZIONI...");  // Pulisci il testo
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
