package com.example.labappmobili;


import static com.example.labappmobili.MainActivity.BACKGROUND_PERMISSION;
import static com.example.labappmobili.MainActivity.FINE_LOCATION_PERMISSION;
import static com.example.labappmobili.MainActivity.NOTIFICATION_PERMISSION;
import static com.example.labappmobili.MainActivity.PERMISSION_BACKGROUND_CODE;
import static com.example.labappmobili.MainActivity.PERMISSION_LOCATION_CODE;

import static com.example.labappmobili.MainActivity.PERMISSION_NOTIFICATION_CODE;
import static com.example.labappmobili.MainActivity.RECORD_AUDIO_PERMISSION;
import static com.example.labappmobili.MainActivity.PERMISSION_AUDIO_CODE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
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
import android.widget.Toast;

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

    private Switch switchLocation, switchLte, switchWifi, switchNoise, switchNotification, switchBackground;
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

        switchBackground = findViewById(R.id.switchBackground);

        switchLte = findViewById(R.id.switchLte);
        switchWifi = findViewById(R.id.switchWifi);
        buttonClear = findViewById(R.id.buttonClear);
        imageButton = findViewById(R.id.imageButton);

        checkSwitchLocation();
        checkSwitchAudio();
        checkSwitchNotification();
        checkSwitchWifi();
        checkSwitchLte();
        checkSwitchBackground();


        // Gestisci il cambio di stato degli interruttori
        switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestRuntimeLocation();
            } else {
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
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

                builder.show();
            }
            checkSwitchAudio();
        });

        switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestRuntimePermissionNotification();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.message_notifications)
                        .setTitle(R.string.permission_required)
                        .setCancelable(false)
                        .setPositiveButton(R.string.setting, (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                            startActivity(intent);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

                builder.show();
            }
            checkSwitchNotification();
        });


        switchBackground.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                requestRuntimePermissionBackground();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.message_background)
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
            }
            checkSwitchBackground();
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
        String[] options = {"1m", "5m", "10m", "5s"};

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



        Spinner selectAvg = findViewById(R.id.selectAvg);
        Integer[] avg = { 5, 10, 30};

        ArrayAdapter<Integer> adapterAvg = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, avg);
        adapterAvg.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectAvg.setAdapter(adapterAvg);


        // Ottieni la preferenza corrente dell'utente
        SharedPreferences preferencesSpinnerAvg = getSharedPreferences("PrefAvg", MODE_PRIVATE);
        int measurementIntervalAvg = preferencesSpinnerAvg.getInt("averageValue", 5); // Imposta un valore di default, ad esempio "5s"

        // Trova l'indice della preferenza corrente e imposta lo spinner
        int selectedIndexAvg = Arrays.asList(avg).indexOf(measurementIntervalAvg);
        selectAvg.setSelection(selectedIndexAvg);

        selectAvg.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                int selectedAverage = avg[position];
                SharedPreferences.Editor editor = preferencesSpinnerAvg.edit();
                editor.putInt("averageValue", selectedAverage);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing here
            }
        });




        Spinner selectMapSpinner = findViewById(R.id.selectMap);
        String[] mapOptions = {this.getResources().getString(R.string.map_normal), "Satellite"};

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



    private void requestRuntimePermissionNotification() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, NOTIFICATION_PERMISSION)) {
            // Spiega l'importanza dell'autorizzazione all'utente
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.message_notifications)
                    .setTitle(R.string.permission_required)
                    .setCancelable(false)
                    .setPositiveButton(R.string.setting, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                        startActivity(intent);
                        dialog.dismiss();
                    })
                    .setNegativeButton(R.string.cancel, (dialog, which) -> {
                        dialog.dismiss();
                    });

            builder.show();
        } else {

            if (ActivityCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION) == PackageManager.PERMISSION_DENIED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.message_notifications)
                        .setTitle(R.string.permission_required)
                        .setCancelable(false)
                        .setPositiveButton(R.string.setting, (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                                intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                                startActivity(intent);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                        });

                builder.show();
            }

            //ActivityCompat.requestPermissions(this, new String[]{NOTIFICATION_PERMISSION}, PERMISSION_NOTIFICATION_CODE);
        }
    }



    private void requestRuntimePermissionBackground() {
        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, BACKGROUND_PERMISSION)) {
                // Spiega l'importanza dell'autorizzazione all'utente
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.message_background)
                        .setTitle(R.string.permission_required)
                        .setCancelable(false)
                        .setPositiveButton("Ok", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this, new String[]{BACKGROUND_PERMISSION},
                                    PERMISSION_BACKGROUND_CODE);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                        });

                builder.show();
            } else {
                // Richiedi l'autorizzazione
                ActivityCompat.requestPermissions(this, new String[]{BACKGROUND_PERMISSION}, PERMISSION_BACKGROUND_CODE);
            }

        } else {
            requestRuntimeLocation();
        }
    }


    private void requestRuntimeAudio() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, RECORD_AUDIO_PERMISSION)) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.message_microphone)
                    .setTitle(R.string.permission_required)
                    .setCancelable(false)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        ActivityCompat.requestPermissions(this, new String[]{RECORD_AUDIO_PERMISSION},
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


    private void requestRuntimeLocation() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, FINE_LOCATION_PERMISSION)) {
            // Spiega l'importanza dell'autorizzazione all'utente
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
            // Richiedi l'autorizzazione
            ActivityCompat.requestPermissions(this, new String[]{FINE_LOCATION_PERMISSION}, PERMISSION_LOCATION_CODE);

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

    private void checkSwitchBackground() {
        TextView textBackground = findViewById(R.id.textSwitchBackground);
        if(ActivityCompat.checkSelfPermission(this, BACKGROUND_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            switchBackground.setChecked(true);
            textBackground.setTextColor(getResources().getColor(R.color.purple_200));
        }else {
            switchBackground.setChecked(false);
            textBackground.setTextColor(getResources().getColor(R.color.black));
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
        builder.setMessage(R.string.message_clear_db)
                .setTitle(R.string.title_clear_db)
                .setCancelable(false)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
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
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        builder.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Aggiungi questo controllo per il permesso di posizione
        if (requestCode == PERMISSION_LOCATION_CODE) {
            checkSwitchLocation();
        }else if (requestCode == PERMISSION_AUDIO_CODE) {
            checkSwitchAudio();
        } else if(requestCode == PERMISSION_NOTIFICATION_CODE) {
            checkSwitchNotification();
        } else if(requestCode == PERMISSION_BACKGROUND_CODE) {
            if (ActivityCompat.checkSelfPermission(this, BACKGROUND_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(R.string.messsage_background_always)
                        .setTitle(R.string.permission_required)
                        .setCancelable(false)
                        .setPositiveButton("Ok", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this, new String[]{BACKGROUND_PERMISSION},
                                    PERMISSION_BACKGROUND_CODE);
                            dialog.dismiss();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> {
                            dialog.dismiss();
                        });

                builder.show();
            }
            checkSwitchBackground();
        }
    }

}
