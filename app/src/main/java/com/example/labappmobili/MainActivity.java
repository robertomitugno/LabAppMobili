package com.example.labappmobili;


import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
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

    private static final int PERMISSION_LOCATION_CODE = 1;
    private static final String FINE_PERMISSION_CODE = android.Manifest.permission.ACCESS_FINE_LOCATION;

    private static final int PERMISSION_AUDIO_CODE = 2;
    private static final String PERMISSION_RECORD_AUDIO = android.Manifest.permission.RECORD_AUDIO;

    private Handler wifiUpdateHandler;
    private static final int UPDATE_INTERVAL = 1000;

    private WifiSignalManager wifiSignalManager;
    private NoiseSignalManager noiseSignalManager;
    private SearchView mapSearchView;

    private Handler lteUpdateHandler;
    private Handler noiseUpdateHandler;

    Location currentLocation;
    FusedLocationProviderClient fusedLocationProviderClient;
    GoogleMap myMap;
    LatLng currentLatLng;

    private TextView variableText;

    RadioButton lteRadioButton, wifiRadioButton, rumoreRadioButton;
    RadioGroup radioGroup;

    boolean isRadioButtonSelected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mapSearchView = findViewById(R.id.mapSearch);

        // Inizializza le view
        variableText = findViewById(R.id.variableText);



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


        // Collega le variabili ai RadioButton nell'XML
        lteRadioButton = findViewById(R.id.lteRadioButton);
        wifiRadioButton = findViewById(R.id.wifiRadioButton);
        rumoreRadioButton = findViewById(R.id.rumoreRadioButton);

        // Aggiungi un listener per gestire gli eventi di selezione
        radioGroup = findViewById(R.id.radioGroup);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.lteRadioButton) {
                variableText.setText("Misurazione in corso . . .");
                LteSignalManager lteSignalManager = new LteSignalManager(MainActivity.this, currentLocation, myMap);
                lteSignalManager.updateLTELevel();
            } else if (checkedId == R.id.wifiRadioButton) {
                wifiSignalManager = new WifiSignalManager(MainActivity.this, currentLocation, myMap);
                wifiSignalManager.updateWifiSignalStrength();
            } else if (checkedId == R.id.rumoreRadioButton) {
                requestRuntimePermissionAudio();
            }
        });



        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Inizializza la mappa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Button noiseButton = findViewById(R.id.record_audio);

        noiseButton.setOnClickListener(v -> {
            noiseUpdateHandler = new Handler();
            // Richiedi l'autorizzazione per l'audio quando il pulsante viene premuto
            requestRuntimePermissionAudio();
        });
        // Richiedi l'autorizzazione per la posizione
        findViewById(R.id.my_location).setOnClickListener(v -> requestRuntimePermissionLocation());

    }


    private void requestRuntimePermissionLocation() {
        if (ActivityCompat.checkSelfPermission(this, FINE_PERMISSION_CODE) == PackageManager.PERMISSION_GRANTED) {
            // Se l'autorizzazione è già concessa, ottieni la posizione
            getLocation();
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_PERMISSION_CODE)) {
            // Spiega l'importanza dell'autorizzazione all'utente
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Questa app richiede l'autorizzazione per la posizione per mostrare le funzionalità.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{FINE_PERMISSION_CODE},
                                PERMISSION_LOCATION_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    });

            builder.show();
        } else {
            // Richiedi l'autorizzazione
            ActivityCompat.requestPermissions(this, new String[]{FINE_PERMISSION_CODE}, PERMISSION_LOCATION_CODE);
        }
    }

    private void requestRuntimePermissionAudio() {
        if (ActivityCompat.checkSelfPermission(this, PERMISSION_RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted. ", Toast.LENGTH_SHORT).show();

            //TODO mettere controllo localizzazione prima della richiesta dell'audio.
            noiseSignalManager = new NoiseSignalManager(this, currentLocation, myMap);
            noiseSignalManager.updateNoiseLevel();

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_RECORD_AUDIO)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Questa app richiede RECORD_AUDIO per rilevare il rumore circostante.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{PERMISSION_RECORD_AUDIO},
                                PERMISSION_AUDIO_CODE);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", ((dialog, which) -> {
                                dialog.dismiss();
                            })
                    );

            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{PERMISSION_RECORD_AUDIO}, PERMISSION_AUDIO_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Se l'autorizzazione è stata concessa, ottieni la posizione
                getLocation();
            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_PERMISSION_CODE)) {
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
                noiseSignalManager = new NoiseSignalManager(this, currentLocation, myMap);
                noiseSignalManager.updateNoiseLevel();

            } else if (!ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_RECORD_AUDIO)) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Questa app richiede RECORD_AUDIO per rilevare il rumore circostante.")
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
        // Posiziona la mappa sulla posizione corrente
        if (currentLatLng != null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                myMap.setMyLocationEnabled(true);
            }
        }
    }


    private void getLocation() {
        // Ottieni l'ultima posizione concesso

        if (ActivityCompat.checkSelfPermission(this, FINE_PERMISSION_CODE) == PackageManager.PERMISSION_GRANTED) {
            Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
            locationTask.addOnSuccessListener(location -> {
                if (location != null) {
                    // Aggiorna la mappa con la posizione corrente
                    currentLocation = location;
                    currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    //Log.d("posizione" , "lat : " + currentLocation.getLongitude() + " long " + currentLocation.getLatitude());
                    onMapReady(myMap);

                } else {
                    Toast.makeText(MainActivity.this, "Location not available.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
