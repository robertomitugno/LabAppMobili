package com.example.labappmobili;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Build;
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

public class NotificationWorker extends Worker {
    private FusedLocationProviderClient fusedLocationClient;

    private Location currentLocation = null;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("prova", "PeriodicWork doWork");

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(super.getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // Start the background location service
            getLocation();

            while (currentLocation == null) {
            }

            if (GridTileProvider.checkEmptyArea(currentLocation, LteSignalManager.getAllLteValue())) {
                sendNotification("Background Task", "Sei entrato in una zona non ancora misurata con il sengale LTE. Aggiorna la tua mappa!");
            } else if (GridTileProvider.checkEmptyArea(currentLocation, WifiSignalManager.getAllWifiValue())){
                sendNotification("Background Task", "Sei entrato in una zona non ancora misurata con il segnale WiFi. Aggiorna la tua mappa!");
            } else if (GridTileProvider.checkEmptyArea(currentLocation, NoiseSignalManager.getAllNoiseValue())) {
                sendNotification("Background Task", "Sei entrato in una zona non ancora misurata con il rumore. Aggiorna la tua mappa!");
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
            channel.setDescription("WorkManger");
            mNotificationManager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), "1")
                .setSmallIcon(R.mipmap.ic_launcher) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(message)// message for notification
                .setAutoCancel(true); // clear notification after click
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
                    Log.e("prova", "FALLITO");

                }
            });

        } catch (Exception e) {
            // Error while doing work..
            Log.e("prova", "EXCEPTION WHILE GETTING LOCATION");
        }

    }
}
