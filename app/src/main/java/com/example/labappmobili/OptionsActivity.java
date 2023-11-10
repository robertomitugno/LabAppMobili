package com.example.labappmobili;


import static com.example.labappmobili.MainActivity.FINE_LOCATION_PERMISSION;
import static com.example.labappmobili.MainActivity.NOTIFICATION_PERMISSION;
import static com.example.labappmobili.MainActivity.PERMISSION_LOCATION_CODE;

import static com.example.labappmobili.MainActivity.PERMISSION_NOTIFICATION_CODE;
import static com.example.labappmobili.MainActivity.RECORD_AUDIO_PERMISSION;
import static com.example.labappmobili.MainActivity.PERMISSION_AUDIO_CODE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.room.Room;

import com.example.labappmobili.RoomDB.LTE.LTEDB;
import com.example.labappmobili.RoomDB.LTE.LTEDao;
import com.example.labappmobili.RoomDB.Noise.NoiseDB;
import com.example.labappmobili.RoomDB.Noise.NoiseDao;
import com.example.labappmobili.RoomDB.WiFi.WiFiDB;
import com.example.labappmobili.RoomDB.WiFi.WiFiDao;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OptionsActivity extends AppCompatActivity {

    private Switch switchLocation, switchLte, switchWifi, switchNoise, switchNotification;
    private TextView textClear, textMisurazioni;
    private ImageButton imageButton;
    private Button buttonClear;

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options_layout);

        SharedPreferences preferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);

        // Inizializza gli elementi UI
        switchLocation = findViewById(R.id.switchLocation);
        switchNoise = findViewById(R.id.switchNoise);

        switchNotification = findViewById(R.id.switchNotification);

        switchLte = findViewById(R.id.switchLte);
        switchWifi = findViewById(R.id.switchWifi);
        textClear = findViewById(R.id.textClear);
        buttonClear = findViewById(R.id.buttonClear);
        imageButton = findViewById(R.id.imageButton);
        textMisurazioni = findViewById(R.id.textMisurazioni);

        checkSwitchLocation();
        checkSwitchAudio();
        checkSwitchNotification();
        checkSwitchWifi();
        checkSwitchLte();


        // Gestisci il cambio di stato degli interruttori
        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestRuntimeLocation();
            } else {
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
            }
            checkSwitchLocation();
        });

        switchLte.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Abilita l'accesso alla rete WiFi
                MainActivity.isLteEnabled = true;
            } else {
                // Disabilita l'accesso alla rete WiFi
                MainActivity.isLteEnabled = false;
            }

            // Salva lo stato nello SharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isLteEnabled", MainActivity.isLteEnabled);
            editor.apply();
            checkSwitchLte();
        });

        switchWifi.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Abilita l'accesso alla rete WiFi
                MainActivity.isWifiEnabled = true;
            } else {
                // Disabilita l'accesso alla rete WiFi
                MainActivity.isWifiEnabled = false;
            }

            // Salva lo stato nello SharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("isWifiEnabled", MainActivity.isWifiEnabled);
            editor.apply();
            checkSwitchWifi();
        });


        switchNoise.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestRuntimeAudio();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Questa app richiede l'autorizzazione per il microfono per mostrare le funzionalità.")
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
            }
            checkSwitchAudio();
        });

        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestRuntimeNotification();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Questa app richiede l'autorizzazione per il notifiche per mostrare le funzionalità.")
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
            }
            checkSwitchNotification();
        });


        //Torna alla MainActivity
        imageButton.setOnClickListener(v -> {
            // Crea un Intent per avviare la MainActivity
            Intent intent = new Intent(OptionsActivity.this, MainActivity.class);
            startActivity(intent);
            // Chiudi l'activity corrente (OptionsActivity)
            finish();
        });


        // Gestisci il clic sul pulsante "Svuota"
        buttonClear.setOnClickListener(v -> clearAllDatabases());

        Spinner selectSpinner = findViewById(R.id.select);
        String[] options = {"5s", "10s", "30s", "1m"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectSpinner.setAdapter(adapter);


        // Ottieni la preferenza corrente dell'utente
        SharedPreferences preferencesSpinner = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String measurementInterval = preferencesSpinner.getString("measurementInterval", "5s"); // Imposta un valore di default, ad esempio "5s"

        // Trova l'indice della preferenza corrente e imposta lo spinner
        int selectedIndex = Arrays.asList(options).indexOf(measurementInterval);
        selectSpinner.setSelection(selectedIndex);

        selectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedInterval = options[position];
                SharedPreferences.Editor editor = preferencesSpinner.edit();
                editor.putString("measurementInterval", selectedInterval);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });




        Spinner selectMapSpinner = findViewById(R.id.selectMap);
        String[] mapOptions = {"Normal", "Satellite"};

        ArrayAdapter<String> mapAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mapOptions);
        mapAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectMapSpinner.setAdapter(mapAdapter);

        // Ottieni la preferenza corrente dell'utente per il tipo di mappa
        SharedPreferences preferencesMap = getSharedPreferences("PrefMap", MODE_PRIVATE);
        String selectedMapType = preferencesMap.getString("selectedMapType", "Normal"); // Imposta un valore di default, ad esempio "Normale"

        // Trova l'indice della preferenza corrente e imposta lo spinner
        int selectedMapIndex = Arrays.asList(mapOptions).indexOf(selectedMapType);
        selectMapSpinner.setSelection(selectedMapIndex);

        selectMapSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String selectedMap = mapOptions[position];
                SharedPreferences.Editor editor = preferencesMap.edit();
                editor.putString("selectedMapType", selectedMap);
                editor.apply();
                // Qui puoi gestire il cambio di tipo di mappa, ad esempio aggiornando la visualizzazione della mappa.
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });
    }



    private void requestRuntimeNotification() {
        if (ActivityCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // Il permesso è già concesso, puoi procedere con le notifiche
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, NOTIFICATION_PERMISSION)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Questa app richiede l'autorizzazione per il notifiche per mostrare le funzionalità.")
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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Questa app richiede l'autorizzazione per il notifiche per mostrare le funzionalità.")
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
        }
        checkSwitchNotification();
    }


    private void requestRuntimeAudio() {
        if (ActivityCompat.checkSelfPermission(this, RECORD_AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED) {

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO_PERMISSION)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Questa app richiede il permesso per il microfono per rilevare il rumore circostante.")
                    .setTitle("Permission Required")
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO_PERMISSION},
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

    private void requestRuntimeLocation() {
        if (ActivityCompat.checkSelfPermission(this, MainActivity.FINE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // Richiedi l'autorizzazione
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
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_LOCATION_PERMISSION)) {
            // Spiega l'importanza dell'autorizzazione all'utente
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
            // Richiedi l'autorizzazione
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
        }
    }

    private void checkSwitchLocation(){

        TextView textLocation = findViewById(R.id.textLocation);
        if(ActivityCompat.checkSelfPermission(this, FINE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            switchLocation.setChecked(true);
            textLocation.setTextColor(getResources().getColor(R.color.purple_200));
        } else {
            switchLocation.setChecked(false);
            textLocation.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void checkSwitchAudio() {
        TextView textNoise = findViewById(R.id.textNoise);
        if(ActivityCompat.checkSelfPermission(this, RECORD_AUDIO_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            switchNoise.setChecked(true);
            textNoise.setTextColor(getResources().getColor(R.color.purple_200));
        }else {
            switchNoise.setChecked(false);
            textNoise.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void checkSwitchNotification() {
        TextView textNotification = findViewById(R.id.textNotification);
        if(ActivityCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            switchNotification.setChecked(true);
            textNotification.setTextColor(getResources().getColor(R.color.purple_200));
        }else {
            switchNotification.setChecked(false);
            textNotification.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void checkSwitchWifi() {
        TextView textWifi = findViewById(R.id.textWifi);
        if(MainActivity.isWifiEnabled) {
            switchWifi.setChecked(true);
            textWifi.setTextColor(getResources().getColor(R.color.purple_200));
        }else {
            switchWifi.setChecked(false);
            textWifi.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void checkSwitchLte() {
        TextView textLte = findViewById(R.id.textLte);
        if(MainActivity.isLteEnabled) {
            switchLte.setChecked(true);
            textLte.setTextColor(getResources().getColor(R.color.purple_200));
        }else {
            switchLte.setChecked(false);
            textLte.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void clearAllDatabases() {
        // Creare un AlertDialog per chiedere conferma
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Confermare l'eliminazione di tutti i dati dal database?")
                .setTitle("Conferma Eliminazione")
                .setCancelable(false)
                .setPositiveButton("Conferma", (dialog, which) -> {
                    // Conferma selezionata, procedere con l'eliminazione
                    executorService.execute(() -> {
                        LTEDB ltedb = Room.databaseBuilder(OptionsActivity.this, LTEDB.class, "LteDB").build();
                        LTEDao lteDao = ltedb.getLTEDao();
                        lteDao.deleteAllLTE();

                        WiFiDB wiFiDB = Room.databaseBuilder(OptionsActivity.this, WiFiDB.class, "WifiDatabase").build();
                        WiFiDao wiFiDao = wiFiDB.getWiFiDao();
                        wiFiDao.deleteAllWifi();

                        NoiseDB noiseDB = Room.databaseBuilder(OptionsActivity.this, NoiseDB.class, "NoiseDB").build();
                        NoiseDao noiseDao = noiseDB.getNoiseDao();
                        noiseDao.deleteAllNoise();
                    });
                    dialog.dismiss();
                })
                .setNegativeButton("Annulla", (dialog, which) -> dialog.dismiss());

        builder.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Aggiungi questo controllo per il permesso di posizione
        if (requestCode == PERMISSION_LOCATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permesso di posizione concessa
                checkSwitchLocation();
            } else {
                checkSwitchLocation();
            }
        }else if (requestCode == PERMISSION_AUDIO_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkSwitchAudio();
            } else {
                checkSwitchAudio();
            }
        } else if(requestCode == PERMISSION_NOTIFICATION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkSwitchNotification();
            } else {
                checkSwitchNotification();
            }
        }
    }

}
