/*package com.example.labappmobili;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.labappmobili.RoomDB.LTE.LTE;
import com.example.labappmobili.RoomDB.Noise.Noise;
import com.example.labappmobili.RoomDB.WiFi.WiFi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CheckTile extends Worker {
    MyLifecycleHandler myLifecycleHandler;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation = null;
    private Date currentDate = null;
    String title = "Titolo";
    String message = "";


    public CheckTile(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        myLifecycleHandler = (MyLifecycleHandler) context.getApplicationContext();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        Application application = (Application) context.getApplicationContext();
    }

    @SuppressLint("MissingPermission")
    @NonNull
    @Override
    public Result doWork() {
        Log.d("WORKER (2)", "Worker is doing its work!");

        // check if the app is in foreground or in background
        boolean isAppInForeground = MyLifecycleHandler.isAppInForeground();

        // check location permission
        Context applicationContext = getApplicationContext();
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        boolean location = checkBackgroundLocationPermission();

        if(location) {
            Log.d("WORKER (2)", "LOCATION ENABLED, Check Last Measuremts DateTime");
            // get current location
            getCurrentLocation();
            // get current date
            getActualDateTime();

            while (currentLocation == null || currentDate == null) {
                //Log.d("WORKER", "waiting fot location updates...");
            }


            /** check the area of the user, using zoom=14 **/
            /*message += " ";

            // find the last WifiData of the area of currentLocation
            List<WiFi> myWifiData = WifiSignalManager.getAllWifiValue();
            List<LTE> myLteData = LteSignalManager.getAllLteValue();
            List<Noise> myNoiseData = NoiseSignalManager.getAllNoiseValue();

            boolean wifi = checkMeasurements(myWifiData);

            boolean lte = checkMeasurements(myLteData);

            boolean noise = checkMeasurements(myNoiseData);

            if(wifi || lte || noise) {
                message = message.substring(0, message.length()-2);
                message += "!";
                // send the notification
                if(isAppInForeground) {
                    Log.d("WORKER (2)", "App in foreground!");
                    sendNotificationInForeground(title, message);
                } else {
                    Log.d("WORKER (2)", "App in background!");
                    buildAndSendNotification(title, message);
                }
            }


        }
        else {
            Log.d("WORKER (2)", "LOCATION DISABLED, I can't get user location");

            if(isAppInForeground) {
                Log.d("WORKER (2)", "App in foreground!");
                String notificationTitle = title;
                String notificationText = "text";
                sendNotificationInForeground(notificationTitle, notificationText);
            } else {
                Log.d("WORKER (2)", "App in background!");
                buildAndSendNotification(this.getApplicationContext().getString(Integer.parseInt("Check area measurements")), this.getApplicationContext().getString(Integer.parseInt("The location permission is required to be able to check measurements in the place where you are!")));
            }
        }
        return Result.success();
    }


    private boolean checkBackgroundLocationPermission() {
        return (ActivityCompat.checkSelfPermission(super.getApplicationContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }


    @SuppressLint("MissingPermission")
    public void getCurrentLocation() {
        Log.d("WORKER (2)", "GET LOCATION...");

        try {
            Task<Location> locationTask = fusedLocationClient.getLastLocation();
            locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        Log.d("WORKER (2)", "LOCATION OBTAINED");

                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        Log.d("WORKER (2)", "CURRENT LOCATION: " + latitude + " - " + longitude);
                        currentLocation = location;
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("WORKER (2)", "LOCATION NOT OBTAINED");
                }
            });

        } catch (Exception e) {
            // Error while doing work..
            Log.e("WORKER (2)", "EXCEPTION WHILE GETTING LOCATION");
        }

    }


    public void getActualDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        String formattedDate = dateFormat.format(date);
        String formattedTime = timeFormat.format(date);
        currentDate = date;
    }


    public void sendNotificationInForeground(String notificationTitle, String notificationText) {
        // send the notification with a Intent

        // put extra in the intent, to set title and text of the notification
        Bundle extras = new Bundle();
        extras.putString("title", notificationTitle);
        extras.putString("text", notificationText);

        Intent sendNotification = new Intent(getApplicationContext(), AppUsageService.class);
        sendNotification.putExtras(extras);
        getApplicationContext().startService(sendNotification);
    }


    @SuppressLint("MissingPermission")
    public void buildAndSendNotification(String title, String text) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                (getApplicationContext(), "notificationServiceChannel")
                .setSmallIcon(R.drawable.noise)
                .setContentTitle(title)
                .setContentText(text)
                .setVibrate(new long[]{1000, 1000})
                .setAutoCancel(true) // notification go away when tapped
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Send Notification to the NotificationManager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        notificationManager.notify(0, mBuilder.build());
    }


    private boolean checkMeasurements(List<?> dataList) {
        GridTileProvider myTileProvider = new GridTileProvider(getApplicationContext(), dataList);


        //boolean findEmptyArea = myTileProvider.checkArea(14);

        boolean findEmptyArea = true;

        //Date lastDate = myTileProvider.getLastDateOfTileStatic(currentLocation, 14);
        //Log.d("WORKER (2) " + signalName + " LAST DATETIME", String.valueOf(lastDate));
        if(findEmptyArea == true) {
            // no measurements in this area, send notification
            //Log.d(signalName, "Send notification to take new mesurement");
            //message += signalName + ", ";
            return true;
        } else {
            // check the actual datetime
            // check dates differs of at least 24h
            /*Calendar calendar1 = Calendar.getInstance();
            calendar1.setTime(lastDate);

            Calendar calendar2 = Calendar.getInstance();
            calendar2.setTime(currentDate);

            // add n_val min to dateTime of the last measurement
            calendar1.add(Calendar.HOUR, 24);

            if (calendar1.before(calendar2)) {
                // yes, send notification
                Log.d(signalName, "Send notification to take new mesurement");
                message += signalName + ", ";
                return true;*/
            //} else {
                // no, do not send notification
                //Log.d(signalName, "NOT send Notification");
                //return false;
            //}
        /*}
    }
}*/
