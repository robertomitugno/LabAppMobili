package com.example.labappmobili;

import static com.example.labappmobili.MainActivity.stopWorker;

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
import androidx.work.WorkManager;
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

    Context context;

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        //Log.d("prova", "PeriodicWork doWork");
        //Log.d("prova",String.valueOf(!ActivityMonitor.isAnyActivityRunning()));
        if(!ActivityMonitor.isAnyActivityRunning()) {
            // Check for location permissions
            if (ActivityCompat.checkSelfPermission(super.getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Start the background location service
                getLocation();

                while (currentLocation == null) {
                }


                if (GridTileProvider.checkEmptyArea(currentLocation, LteSignalManager.getAllLteValue())) {
                    sendNotification(context.getResources().getString(R.string.notification_title), context.getResources().getString(R.string.notification_empty_lte));
                    stopWorker();
                } else if (GridTileProvider.checkEmptyArea(currentLocation, WifiSignalManager.getAllWifiValue())) {
                    sendNotification(context.getResources().getString(R.string.notification_title), context.getResources().getString(R.string.notification_empty_wifi));
                    stopWorker();
                } else if (GridTileProvider.checkEmptyArea(currentLocation, NoiseSignalManager.getAllNoiseValue())) {
                    sendNotification(context.getResources().getString(R.string.notification_title), context.getResources().getString(R.string.notification_empty_noise));
                    stopWorker();
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
                    Log.e("NotificationWorker", "Failure getting background location");

                }
            });

        } catch (Exception e) {
            // Error while doing work..
            Log.e("NotificationWorker", "Exception gettin background location");
        }

    }
}
