package com.example.labappmobili;

import static com.example.labappmobili.MainActivity.getIntervalInMillis;
import static com.example.labappmobili.MainActivity.stopWorker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class BackgroundWorker extends Worker {
    private FusedLocationProviderClient fusedLocationClient;

    private Location currentLocation = null;

    Context context;

    public static final String FINE_LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION;

    public BackgroundWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @NonNull
    @Override
    public Result doWork() {

        SharedPreferences preferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String measurementInterval = preferences.getString("measurementInterval", "5s");
        boolean isBackgroundMeasureEnabled = preferences.getBoolean("isBackgroundMeasureEnabled", false);

        if(!ActivityMonitor.isAnyActivityRunning()) {
            // Check for location permissions
            if (ActivityCompat.checkSelfPermission(super.getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Start the background location service
                getLocation();

                while (currentLocation == null) {
                }

                if (GridTileProvider.checkEmptyArea(currentLocation, new LteSignalManager(context).getAllLteValue())) {
                    sendNotification(context.getResources().getString(R.string.notification_title), context.getResources().getString(R.string.notification_empty_lte));
                    stopWorker();
                } else if (GridTileProvider.checkEmptyArea(currentLocation, new WifiSignalManager(context).getAllWifiValue())) {
                    sendNotification(context.getResources().getString(R.string.notification_title), context.getResources().getString(R.string.notification_empty_wifi));
                    stopWorker();
                } else if (GridTileProvider.checkEmptyArea(currentLocation, new NoiseSignalManager(context).getAllNoiseValue())) {
                    sendNotification(context.getResources().getString(R.string.notification_title), context.getResources().getString(R.string.notification_empty_noise));
                    stopWorker();
                }

            }
        } else{
            if (ActivityCompat.checkSelfPermission(super.getApplicationContext(), FINE_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED) {

                // Start the background location service
                getLocation();

                while (currentLocation == null) {  }
                if (isBackgroundMeasureEnabled) {

                    if (GridTileProvider.checkEmptyArea(currentLocation, new LteSignalManager(context).getAllLteValue())) {
                        LteSignalManager.insertLTEMeasurement(currentLocation, getIntervalInMillis(measurementInterval));
                    }
                    if (GridTileProvider.checkEmptyArea(currentLocation, new WifiSignalManager(context).getAllWifiValue())) {
                        WifiSignalManager.insertWifiMeasurement(currentLocation, getIntervalInMillis(measurementInterval));
                    }
                    if (GridTileProvider.checkEmptyArea(currentLocation, new NoiseSignalManager(context).getAllNoiseValue())) {
                        NoiseSignalManager.insertNoiseMeasurement(currentLocation, getIntervalInMillis(measurementInterval));
                    }
                }


            }
        }

        return Result.success();
    }



    void sendNotification(String title, String message) {
        NotificationManager mNotificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1",
                    "android",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("WorkManager");
            mNotificationManager.createNotificationChannel(channel);
        }

        // Utilizzo di BigTextStyle per estendere il riquadro della notifica
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "1")
                .setSmallIcon(R.drawable.app_image) // icona della notifica
                .setContentTitle(title) // titolo della notifica
                .setContentText(message) // testo della notifica
                .setAutoCancel(true) // cancella la notifica dopo il clic
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message)); // utilizzo di BigTextStyle

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pi);
        mNotificationManager.notify(0, mBuilder.build());
    }


    @SuppressLint("MissingPermission")
    public void getLocation() {

        try {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        currentLocation = location;
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("NotificationWorker", "Failure getting background location");

                }
            });

        } catch (Exception e) {
            // Error while doing work..
            Log.e("NotificationWorker", "Exception gettin background location");
        }

    }
}
