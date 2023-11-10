/*package com.example.labappmobili;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

/**
 * Service used to Build and Send a Notification
 * **/
/*public class AppUsageService extends Service {
    public AppUsageService() {     }
    Handler handler;

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(() -> {
            // get Extras
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String notificationTitle = extras.getString("title");
                String notificationText = extras.getString("text");
                // Build and Send the Notification
                sendNotifications(notificationTitle, notificationText);
            }


            handler.post(() -> {
                Log.d("SEND NOTIFICATION SERVICE", "NOTIFICATION SENT");
                stopSelf();
            });

        }).start();

        return START_STICKY;
    }


    // Build and Send the Notification
    @SuppressLint("MissingPermission")
    public void sendNotifications(String title, String text) {
        Log.d("NOTIFICATIONS", "Building Notification");

        // Intent: Receiving Activity
        Intent intentCaller = new Intent(this, MainActivity.class);
        intentCaller.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intentCaller.putExtra("CALLER", "open");

        // PendingIntent to be fired when notification is selected
        // the PendingIntent is the container of the Intent that is launch by the SO
        // PendingIntent.getActivity() is like: startActivity -> Activity specified in the Intent newIntent
        PendingIntent openIntent = PendingIntent.getActivity
                (this, 0, intentCaller, PendingIntent.FLAG_IMMUTABLE);

        // Build Notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder
                (this, "notificationServiceChannel")
                .setSmallIcon(R.drawable.noise)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openIntent)
                .setVibrate(new long[]{ 1000, 1000})
                .setAutoCancel(true) // notification go away when tapped
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        // Send Notification to the NotificationManager
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(0, mBuilder.build());
    }

}*/
